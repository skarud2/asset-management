package com.via.shinvia.futuresim.controller;

import com.via.shinvia.futuresim.dto.response.AgeCohortSpectrumResponse;
import com.via.shinvia.futuresim.dto.response.GoalPreviewResponse;
import com.via.shinvia.futuresim.dto.response.HouseholdCohortSpectrumResponse;
import com.via.shinvia.futuresim.dto.response.HouseholdProfileSummaryResponse;
import com.via.shinvia.futuresim.service.CurrentStatusService;
import com.via.shinvia.futuresim.service.FutureSimulationEngine;
import com.via.shinvia.futuresim.service.GoalBenchmarkComparator;
import com.via.shinvia.futuresim.service.GoalPresetService;
import com.via.shinvia.futuresim.service.HouseholdNetWorthBenchmarkService;
import com.via.shinvia.futuresim.service.HouseholdProfileSummaryService;
import com.via.shinvia.security.CurrentUser;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// 미래 금융 시뮬레이터 2단계에서 쓰는 조회 API.
// 1단계에서 세션에 저장한 기준(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY/HOUSEHOLD_SIZE_SESSION_KEY)을
// 그대로 읽어서 쓴다 — 별도 저장소 없음.
@RestController
@RequestMapping("/api/future-goal")
@RequiredArgsConstructor
public class FutureGoalApiController {

    private final HouseholdProfileSummaryService householdProfileSummaryService;
    private final GoalPresetService goalPresetService;
    private final FutureSimulationEngine futureSimulationEngine;
    private final GoalBenchmarkComparator goalBenchmarkComparator;
    private final CurrentStatusService currentStatusService;
    private final HouseholdNetWorthBenchmarkService householdNetWorthBenchmarkService;
    private final CurrentUser currentUser;

    // ⚠️ 절대 규칙: 이 메서드(household-profile-summary)는 이번 작업에서 건드리지 않는다.
    @GetMapping("/household-profile-summary")
    public ResponseEntity<HouseholdProfileSummaryResponse> householdProfileSummary(HttpSession session) {
        String basis = (String) session.getAttribute(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY);

        if (!"HOUSEHOLD".equals(basis)) {
            return ResponseEntity.noContent().build();
        }

        String householdSizeLabel = (String) session.getAttribute(FutureSimViewController.HOUSEHOLD_SIZE_SESSION_KEY);
        String summaryText = householdProfileSummaryService.generateSummary(householdSizeLabel);

        return ResponseEntity.ok(new HouseholdProfileSummaryResponse(summaryText));
    }

    @GetMapping("/presets")
    public ResponseEntity<List<GoalPresetService.GoalPreset>> presets(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(goalPresetService.getPresets(userId));
    }

    @GetMapping("/preview")
    public ResponseEntity<GoalPreviewResponse> preview(
            @RequestParam BigDecimal goalAmount,
            Authentication authentication,
            HttpSession session
    ) {
        Long userId = currentUser.getUserId(authentication);

        CurrentStatusService.CurrentStatusView status = currentStatusService.getCurrentStatus(userId, null);
        BigDecimal currentNetWorth = status.mySnapshot().netWorth() != null
                ? status.mySnapshot().netWorth() : BigDecimal.ZERO;
        FutureSimulationEngine.SavingsCapacity savingsCapacity = futureSimulationEngine.calculateSavingsCapacity(userId);
        Integer monthsToGoal = futureSimulationEngine.calculateMonthsToGoal(userId, goalAmount);
        BenchmarkInfo benchmarkInfo = resolveBenchmarkInfo(userId, goalAmount, session);

        return ResponseEntity.ok(new GoalPreviewResponse(
                monthsToGoal,
                currentNetWorth,
                goalAmount.subtract(currentNetWorth).max(BigDecimal.ZERO),
                savingsCapacity.monthlyIncome(),
                savingsCapacity.monthlyLivingExpense(),
                savingsCapacity.monthlyLoanPayment(),
                savingsCapacity.monthlySavingsCapacity(),
                !savingsCapacity.hasSavingsCapacity(),
                benchmarkInfo.comparisonText(),
                benchmarkInfo.label(),
                benchmarkInfo.medianNetWorth()
        ));
    }

    // 연령대 스펙트럼(29세 이하~60세 이상) 5개 구간 전체 + 사용자가 속한 구간의 인덱스를 반환한다.
    // household-profile-summary와 대칭으로, 1단계에서 AGE 기준을 선택했을 때만 내려준다
    // (HOUSEHOLD를 선택했으면 이 스펙트럼 대신 preview의 가구원별 막대 비교가 보여야 하므로 — 두 비교는
    // 서로 다른 기준을 위한 것이지 하나가 다른 하나를 덮어쓰는 게 아니다).
    @GetMapping("/age-cohort-spectrum")
    public ResponseEntity<AgeCohortSpectrumResponse> ageCohortSpectrum(Authentication authentication, HttpSession session) {
        String basis = (String) session.getAttribute(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY);
        if (!"AGE".equals(basis)) {
            return ResponseEntity.noContent().build();
        }

        Long userId = currentUser.getUserId(authentication);
        CurrentStatusService.CurrentStatusView status = currentStatusService.getCurrentStatus(userId, null);

        if (!status.agePanel().available()) {
            return ResponseEntity.noContent().build();
        }

        List<HouseholdNetWorthBenchmarkService.AgeGroupResult> allCohorts =
                householdNetWorthBenchmarkService.getAllAgeGroupBenchmarks();

        String userAgeGroupLabel = status.agePanel().benchmark().ageGroupLabel();
        int userAgeGroupIndex = -1;
        List<AgeCohortSpectrumResponse.CohortPoint> cohortPoints = new ArrayList<>();
        String surveyYear = null;

        for (int i = 0; i < allCohorts.size(); i++) {
            HouseholdNetWorthBenchmarkService.AgeGroupResult cohort = allCohorts.get(i);
            cohortPoints.add(new AgeCohortSpectrumResponse.CohortPoint(cohort.ageGroupLabel(), cohort.medianNetWorth()));
            surveyYear = cohort.surveyYear();
            if (cohort.ageGroupLabel().equals(userAgeGroupLabel)) {
                userAgeGroupIndex = i;
            }
        }

        return ResponseEntity.ok(new AgeCohortSpectrumResponse(
                cohortPoints,
                userAgeGroupIndex,
                status.agePanel().userAge(),
                surveyYear,
                status.mySnapshot().netWorth()
        ));
    }

    // 가구원수 구성비 막대(1인~5인이상) 5개 구간 전체 + 사용자가 속한 구간의 인덱스를 반환한다.
    // age-cohort-spectrum과 대칭으로, 1단계에서 HOUSEHOLD 기준을 선택했을 때만 내려준다.
    @GetMapping("/household-cohort-spectrum")
    public ResponseEntity<HouseholdCohortSpectrumResponse> householdCohortSpectrum(
            Authentication authentication,
            HttpSession session
    ) {
        String basis = (String) session.getAttribute(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY);
        if (!"HOUSEHOLD".equals(basis)) {
            return ResponseEntity.noContent().build();
        }

        Long userId = currentUser.getUserId(authentication);
        String sessionHouseholdSize = (String) session.getAttribute(FutureSimViewController.HOUSEHOLD_SIZE_SESSION_KEY);
        CurrentStatusService.CurrentStatusView status = currentStatusService.getCurrentStatus(userId, sessionHouseholdSize);

        List<HouseholdNetWorthBenchmarkService.Result> allCohorts =
                householdNetWorthBenchmarkService.getAllHouseholdBenchmarks();

        String userHouseholdSizeLabel = status.householdBenchmark().householdSizeLabel();
        int userHouseholdSizeIndex = -1;
        List<HouseholdCohortSpectrumResponse.CohortPoint> cohortPoints = new ArrayList<>();
        String surveyYear = null;

        for (int i = 0; i < allCohorts.size(); i++) {
            HouseholdNetWorthBenchmarkService.Result cohort = allCohorts.get(i);
            cohortPoints.add(new HouseholdCohortSpectrumResponse.CohortPoint(
                    cohort.householdSizeLabel(), cohort.medianNetWorth(), cohort.householdDistributionPct()
            ));
            surveyYear = cohort.surveyYear();
            if (cohort.householdSizeLabel().equals(userHouseholdSizeLabel)) {
                userHouseholdSizeIndex = i;
            }
        }

        // 1단계에서 가구원수를 아예 입력하지 않았으면(전체 기준 폴백) 스펙트럼에서 강조할 구간이 없다 —
        // 이 경우는 프론트에서 빈 화면 대신 "가구원수를 골라야 강조 표시가 된다" 정도로 처리하게 인덱스를 그대로 -1로 둔다.
        return ResponseEntity.ok(new HouseholdCohortSpectrumResponse(
                cohortPoints,
                userHouseholdSizeIndex,
                surveyYear
        ));
    }

    // 1단계에서 선택한 기준(AGE/HOUSEHOLD)에 맞는 라벨·중앙값 순자산·비교 문구를 한 번에 모은다.
    // 기준을 선택하지 않았으면(건너뛰기) 전부 null.
    private BenchmarkInfo resolveBenchmarkInfo(Long userId, BigDecimal goalAmount, HttpSession session) {
        String basis = (String) session.getAttribute(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY);
        if (basis == null) {
            return BenchmarkInfo.EMPTY;
        }

        String sessionHouseholdSize = (String) session.getAttribute(FutureSimViewController.HOUSEHOLD_SIZE_SESSION_KEY);
        CurrentStatusService.CurrentStatusView status = currentStatusService.getCurrentStatus(userId, sessionHouseholdSize);

        if ("AGE".equals(basis)) {
            if (!status.agePanel().available()) {
                return BenchmarkInfo.EMPTY;
            }
            HouseholdNetWorthBenchmarkService.AgeGroupResult benchmark = status.agePanel().benchmark();
            String text = goalBenchmarkComparator.compareToBenchmark(goalAmount, benchmark.medianNetWorth(), benchmark.ageGroupLabel());
            return new BenchmarkInfo(text, benchmark.ageGroupLabel(), benchmark.medianNetWorth());
        }

        if ("HOUSEHOLD".equals(basis)) {
            HouseholdNetWorthBenchmarkService.Result benchmark = status.householdBenchmark();
            String text = goalBenchmarkComparator.compareToBenchmark(goalAmount, benchmark.medianNetWorth(), benchmark.householdSizeLabel());
            return new BenchmarkInfo(text, benchmark.householdSizeLabel(), benchmark.medianNetWorth());
        }

        return BenchmarkInfo.EMPTY;
    }

    private record BenchmarkInfo(String comparisonText, String label, BigDecimal medianNetWorth) {
        static final BenchmarkInfo EMPTY = new BenchmarkInfo(null, null, null);
    }
}
