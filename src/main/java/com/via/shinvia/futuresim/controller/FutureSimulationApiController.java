package com.via.shinvia.futuresim.controller;

import com.via.shinvia.futuresim.dto.request.ComboSimulationRequest;
import com.via.shinvia.futuresim.dto.request.PlanSaveRequest;
import com.via.shinvia.futuresim.dto.response.ComboSimulationResponse;
import com.via.shinvia.futuresim.dto.response.FutureSimulationProjectionResponse;
import com.via.shinvia.futuresim.dto.response.LeverCombinationResponse;
import com.via.shinvia.futuresim.dto.response.LeverImpactResponse;
import com.via.shinvia.futuresim.dto.response.LeverLoanSummaryResponse;
import com.via.shinvia.futuresim.dto.response.LeverIntensityCurveResponse;
import com.via.shinvia.futuresim.dto.response.LeverIntensityResponse;
import com.via.shinvia.futuresim.dto.response.PlanSaveResponse;
import com.via.shinvia.futuresim.dto.response.PlanSummaryResponse;
import com.via.shinvia.futuresim.dto.response.RateRiskReferenceResponse;
import com.via.shinvia.futuresim.dto.response.RecommendedComboResponse;
import com.via.shinvia.futuresim.event.RateChangeMode;
import com.via.shinvia.futuresim.service.ComboSimulationService;
import com.via.shinvia.futuresim.service.CurrentStatusService;
import com.via.shinvia.futuresim.service.FutureSimulationEngine;
import com.via.shinvia.futuresim.service.LeverCombinationOptimizer;
import com.via.shinvia.futuresim.service.LeverIntensityCalculator;
import com.via.shinvia.futuresim.service.LeverLoanComparisonService;
import com.via.shinvia.futuresim.service.PlanSnapshotService;
import com.via.shinvia.futuresim.service.RateRiskReferenceService;
import com.via.shinvia.futuresim.service.RecommendedComboService;
import com.via.shinvia.futuresim.service.AppConfigService;
import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.service.DebtPriorityService;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import com.via.shinvia.security.CurrentUser;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 미래 금융 시뮬레이터 3단계("성장 곡선")에서 쓰는 조회 API.
// 1단계에서 세션에 저장한 비교 기준(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY/
// HOUSEHOLD_SIZE_SESSION_KEY)을 FutureGoalApiController와 동일한 방식으로 그대로 읽어서 쓴다.
@RestController
@RequestMapping("/api/future-simulation")
@RequiredArgsConstructor
public class FutureSimulationApiController {

    // FutureSimulationEngine의 시뮬레이션 상한(20년)과 같은 값 — "도달 불가(null)"를 NOT NULL 컬럼에
    // 저장해야 할 때 쓰는 sentinel.
    private static final int PROJECTION_UNREACHABLE_SENTINEL_MONTHS = 1200;

    private final CurrentUser currentUser;
    private final FutureSimulationEngine futureSimulationEngine;
    private final CurrentStatusService currentStatusService;
    private final LeverIntensityCalculator leverIntensityCalculator;
    private final LeverLoanComparisonService leverLoanComparisonService;
    private final RateRiskReferenceService rateRiskReferenceService;
    private final LeverCombinationOptimizer leverCombinationOptimizer;
    private final ComboSimulationService comboSimulationService;
    private final PlanSnapshotService planSnapshotService;
    private final RecommendedComboService recommendedComboService;
    private final AppConfigService appConfigService;
    private final StressTestLoanMapper stressTestLoanMapper;
    // loananalysis/(팀원 도메인)의 기존 대출 우선순위 로직을 읽기 전용으로 재사용한다 — 수정 없음.
    private final DebtPriorityService debtPriorityService;

    @GetMapping("/projection")
    public ResponseEntity<FutureSimulationProjectionResponse> projection(
            @RequestParam BigDecimal goalAmount,
            @RequestParam(required = false) BigDecimal assumedReturnRate,
            Authentication authentication,
            HttpSession session
    ) {
        Long userId = currentUser.getUserId(authentication);

        FutureSimulationEngine.Projection projection = assumedReturnRate == null
                ? futureSimulationEngine.calculateProjection(userId, goalAmount)
                : futureSimulationEngine.calculateProjection(userId, goalAmount, assumedReturnRate.max(BigDecimal.ZERO).min(new BigDecimal("10")));
        List<FutureSimulationProjectionResponse.TimelinePoint> timeline = projection.timeline().stream()
                .map(point -> new FutureSimulationProjectionResponse.TimelinePoint(
                        point.monthOffset(), point.netWorth(), point.contributionAmount(), point.returnAmount()))
                .toList();

        BenchmarkInfo benchmarkInfo = resolveBenchmarkInfo(userId, session);
        Integer benchmarkCrossMonth = benchmarkInfo.medianNetWorth() == null
                ? null
                : findFirstCrossMonth(projection.timeline(), benchmarkInfo.medianNetWorth());

        return ResponseEntity.ok(new FutureSimulationProjectionResponse(
                projection.monthsToGoal(),
                timeline,
                goalAmount,
                benchmarkInfo.medianNetWorth(),
                benchmarkInfo.label(),
                benchmarkCrossMonth,
                projection.crossoverReached(),
                projection.savingsCapacity().monthlyIncome(),
                projection.savingsCapacity().monthlyLivingExpense(),
                projection.savingsCapacity().monthlyLoanPayment(),
                projection.savingsCapacity().monthlySavingsCapacity(),
                projection.assumedReturnRatePercent(),
                projection.totalDebt()
        ));
    }

    @GetMapping("/recommended-combo")
    public ResponseEntity<RecommendedComboResponse> recommendedCombo(
            @RequestParam BigDecimal goalAmount,
            @RequestParam(required = false) BigDecimal monthlyExtraCapacity,
            @RequestParam(required = false) BigDecimal prepaymentAmount,
            @RequestParam(required = false) BigDecimal termExtensionMonths,
            Authentication authentication
    ) {
        var result = recommendedComboService.getRecommendedCombo(
                currentUser.getUserId(authentication), goalAmount, monthlyExtraCapacity, prepaymentAmount, termExtensionMonths);
        return ResponseEntity.ok(new RecommendedComboResponse(result.levers().stream()
                .map(item -> new RecommendedComboResponse.Lever(item.type(), item.intensity(), item.diffMonths(), item.assumption())).toList(),
                result.baselineMonthsToGoal(), result.comboMonthsToGoal(), result.diffMonths(), result.assumedReturnRate()));
    }

    @GetMapping("/loan-fee-reference")
    public ResponseEntity<java.util.Map<String, Object>> loanFeeReference(Authentication authentication) {
        var loan = stressTestLoanMapper.findNormalLoansByUserId(currentUser.getUserId(authentication)).stream()
                .max(java.util.Comparator.comparing(com.via.shinvia.stresstest.entity.StressTestLoanRow::getCurrentBalance)).orElse(null);
        if (loan == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(java.util.Map.of("feeRate", loan.getPrepaymentFeeRate() == null ? "" : loan.getPrepaymentFeeRate(),
                "feeEndDate", loan.getPrepaymentFeeEndDate() == null ? "" : loan.getPrepaymentFeeEndDate().toString()));
    }

    // 1단계에서 기준을 고르지 않았으면(건너뛰기) 둘 다 null — 화면에서 벤치마크선/마일스톤 줄을 생략한다.
    private BenchmarkInfo resolveBenchmarkInfo(Long userId, HttpSession session) {
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
            return new BenchmarkInfo(
                    status.agePanel().benchmark().medianNetWorth(),
                    status.agePanel().benchmark().ageGroupLabel()
            );
        }
        if ("HOUSEHOLD".equals(basis)) {
            return new BenchmarkInfo(
                    status.householdBenchmark().medianNetWorth(),
                    status.householdBenchmark().householdSizeLabel()
            );
        }
        return BenchmarkInfo.EMPTY;
    }

    private record BenchmarkInfo(BigDecimal medianNetWorth, String label) {
        static final BenchmarkInfo EMPTY = new BenchmarkInfo(null, null);
    }

    private Integer findFirstCrossMonth(List<FutureSimulationEngine.TimelinePoint> timeline, BigDecimal benchmarkNetWorth) {
        for (FutureSimulationEngine.TimelinePoint point : timeline) {
            if (point.netWorth().compareTo(benchmarkNetWorth) >= 0) {
                return point.monthOffset();
            }
        }
        return null;
    }

    // ==== 4단계("레버 랭킹") ====

    // [섹션1] 레버 4종을 기본 강도로 실행해 추천 카드 목록을 만든다. 대출이 없으면 대출 의존 레버(조기상환/
    // 만기연장)는 available=false로 내려가고, 화면에서 그 레버는 목록에서 제외한다.
    @GetMapping("/lever-impact")
    public ResponseEntity<LeverImpactResponse> leverImpact(
            @RequestParam BigDecimal goalAmount, @RequestParam(required = false) BigDecimal assumedReturnRate, Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        var baseline = leverLoanComparisonService.baseline(userId);
        List<LeverImpactResponse.LeverItem> levers = Arrays.stream(LeverIntensityCalculator.LeverType.values())
                .map(leverType -> {
                    boolean available = leverIntensityCalculator.isLeverAvailable(userId, leverType);
                    BigDecimal defaultIntensity = leverIntensityCalculator.defaultIntensityFor(leverType);
                    Integer diffMonths = available
                            ? leverIntensityCalculator.calculateDiffMonths(userId, goalAmount, leverType, defaultIntensity, assumedReturnRate)
                            : null;
                    List<LeverImpactResponse.LeverItem.PresetItem> presets = available
                            ? leverIntensityCalculator.presetIntensitiesFor(userId, leverType).stream()
                                    .map(intensity -> new LeverImpactResponse.LeverItem.PresetItem(
                                            intensity,
                                            leverIntensityCalculator.calculateDiffMonths(userId, goalAmount, leverType, intensity, assumedReturnRate),
                                            leverIntensityCalculator.detailFor(userId, leverType, intensity)
                                    ))
                                    .toList()
                            : List.of();
                    var summary = leverLoanComparisonService.forLever(userId, leverType, defaultIntensity);
                    var loanSummary = loanSummary(summary, baseline);
                    return new LeverImpactResponse.LeverItem(leverType, available, defaultIntensity, diffMonths, presets,
                            loanSummary,
                            loanSummary.monthlyBurden(), loanSummary.monthlyBurdenDiff(),
                            loanSummary.totalInterest(), loanSummary.totalInterestDiff(),
                            loanSummary.repaymentPeriodMonths(), loanSummary.repaymentPeriodDiff());
                })
                .toList();
        BigDecimal representativeLoanRatePercent = leverIntensityCalculator.representativeLoanRatePercent(userId);
        return ResponseEntity.ok(new LeverImpactResponse(levers, representativeLoanRatePercent,
                loanSummary(baseline, baseline)));
    }

    private LeverImpactResponse.LoanSummary loanSummary(
            LeverLoanComparisonService.Summary summary, LeverLoanComparisonService.Summary baseline
    ) {
        return new LeverImpactResponse.LoanSummary(
                summary.monthlyBurden(), summary.monthlyBurden().subtract(baseline.monthlyBurden()),
                summary.totalInterest(), summary.totalInterest().subtract(baseline.totalInterest()),
                summary.repaymentPeriodMonths(), summary.repaymentPeriodMonths() - baseline.repaymentPeriodMonths()
        );
    }

    @GetMapping("/lever-loan-summary")
    public ResponseEntity<LeverLoanSummaryResponse> leverLoanSummary(
            @RequestParam BigDecimal goalAmount, @RequestParam LeverIntensityCalculator.LeverType leverType,
            @RequestParam BigDecimal intensity, @RequestParam(required = false) BigDecimal assumedReturnRate,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        var baseline = leverLoanComparisonService.baseline(userId);
        var summary = leverLoanComparisonService.forLever(userId, leverType, intensity);
        return ResponseEntity.ok(new LeverLoanSummaryResponse(leverType, intensity,
                leverIntensityCalculator.calculateDiffMonths(userId, goalAmount, leverType, intensity, assumedReturnRate),
                loanSummary(summary, baseline)));
    }

    // [섹션1 카드] 강도 칩 3개(최소/기본/최대) 외에 사용자가 "직접 입력"한 값 하나를 계산한다.
    // 유효 범위(최소~최대 강도) 밖으로 들어오면 서버에서 clamp해서 계산한다.
    @GetMapping("/lever-intensity")
    public ResponseEntity<LeverIntensityResponse> leverIntensity(
            @RequestParam BigDecimal goalAmount,
            @RequestParam LeverIntensityCalculator.LeverType leverType,
            @RequestParam BigDecimal intensity,
            @RequestParam(required = false) BigDecimal assumedReturnRate,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        List<BigDecimal> range = leverIntensityCalculator.intensityPointsFor(userId, leverType);
        BigDecimal clamped = intensity.max(range.get(0)).min(range.get(range.size() - 1));

        Integer diffMonths = leverIntensityCalculator.calculateDiffMonths(userId, goalAmount, leverType, clamped, assumedReturnRate);
        LeverIntensityCalculator.LeverDetail detail = leverIntensityCalculator.detailFor(userId, leverType, clamped);
        return ResponseEntity.ok(new LeverIntensityResponse(leverType, clamped, diffMonths, detail));
    }

    // [섹션1 카드 펼침] 카드를 펼치는 시점에 전체 강도-효과 곡선을 한 번에 받는다 — 미니 효과 곡선과
    // 효과 둔화 지점(diminishingReturnIntensity) 표시에 쓴다.
    @GetMapping("/lever-intensity-curve")
    public ResponseEntity<LeverIntensityCurveResponse> leverIntensityCurve(
            @RequestParam BigDecimal goalAmount,
            @RequestParam LeverIntensityCalculator.LeverType leverType,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        LeverIntensityCalculator.IntensityCurve curve =
                leverIntensityCalculator.calculateIntensityCurve(userId, goalAmount, leverType);
        List<LeverIntensityCurveResponse.Point> points = curve.points().stream()
                .map(point -> new LeverIntensityCurveResponse.Point(point.intensity(), point.diffMonths()))
                .toList();
        return ResponseEntity.ok(new LeverIntensityCurveResponse(leverType, points, curve.diminishingReturnIntensity()));
    }

    // [섹션2] 금리 리스크 참고 카드 — 3가지 모드 탭 전환 시 호출.
    @GetMapping("/rate-risk-reference")
    public ResponseEntity<RateRiskReferenceResponse> rateRiskReference(
            @RequestParam(defaultValue = "SIMPLE") RateChangeMode mode, Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(rateRiskReferenceService.calculate(userId, mode));
    }

    // [섹션1 하단] 레버 4종을 동시에 썼을 때 가장 빠른 조합을 찾는다(DP, LeverCombinationOptimizer 참고).
    @GetMapping("/lever-combination")
    public ResponseEntity<LeverCombinationResponse> leverCombination(
            @RequestParam BigDecimal goalAmount, Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        LeverCombinationOptimizer.CombinationResult result = leverCombinationOptimizer.findOptimalCombination(userId, goalAmount);
        List<LeverCombinationResponse.ChosenLever> chosenLevers = result.chosenLevers().stream()
                .map(choice -> new LeverCombinationResponse.ChosenLever(choice.leverType(), choice.intensity()))
                .toList();
        return ResponseEntity.ok(new LeverCombinationResponse(
                chosenLevers, result.baselineMonths(), result.combinedMonths(), result.diffMonths(),
                result.liquidAssetBudget(), result.combinationsEvaluated()
        ));
    }

    // [섹션1 하단] 보유 대출이 2개 이상일 때만 "이 대출부터 갚으세요" 우선순위를 보여준다.
    // loananalysis/DebtPriorityService(연체·이자·수수료·소액·학자금 가중합 점수)를 그대로 재사용 — 수정 없음.
    @GetMapping("/debt-priorities")
    public ResponseEntity<List<DebtPriorityResponseDTO>> debtPriorities(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        List<DebtPriorityResponseDTO> priorities = debtPriorityService.calculate(userId);
        return ResponseEntity.ok(priorities.size() >= 2 ? priorities : List.of());
    }

    // ==== 5단계("레버 조합해보기") ====

    // 체크한 레버 여러 개(0개 포함)를 한 번에 FutureSimulationEngine에 투입해서 3단계와 같은 형태의
    // timeline을 돌려준다. 3단계 성장곡선 위에 이 결과를 겹쳐 그리는 데 쓴다.
    @PostMapping("/combo-simulation")
    public ResponseEntity<ComboSimulationResponse> comboSimulation(
            @RequestBody ComboSimulationRequest request, Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        List<LeverIntensityCalculator.LeverSelection> selections = request.levers() == null
                ? List.of()
                : request.levers().stream()
                        .map(entry -> new LeverIntensityCalculator.LeverSelection(entry.type(), entry.intensity()))
                        .toList();

        ComboSimulationService.ComboResult result = comboSimulationService.simulate(userId, request.goalAmount(), selections, request.assumedReturnRate());
        List<ComboSimulationResponse.TimelinePoint> timeline = result.timeline().stream()
                .map(point -> new ComboSimulationResponse.TimelinePoint(
                        point.monthOffset(), point.netWorth(), point.contributionAmount(), point.returnAmount()))
                .toList();

        return ResponseEntity.ok(new ComboSimulationResponse(
                result.baselineMonthsToGoal(), result.comboMonthsToGoal(), result.diffMonths(), timeline
        ));
    }

    @PostMapping(value = "/loan-impact", produces = "application/json")
    public ResponseEntity<String> loanImpact(@RequestBody ComboSimulationRequest request, Authentication authentication) {
        List<LeverIntensityCalculator.LeverSelection> selections = request.levers() == null ? List.of() : request.levers().stream()
                .map(item -> new LeverIntensityCalculator.LeverSelection(item.type(), item.intensity())).toList();
        return ResponseEntity.ok(buildLoanImpactJson(currentUser.getUserId(authentication), selections));
    }

    // "다음"(6단계로 이동) 클릭 시 현재 계획을 저장한다. 서버에서 다시 시뮬레이션을 돌려서 저장하므로
    // 클라이언트가 보낸 개월수/순자산을 그대로 믿지 않는다 — combo-simulation과 항상 같은 계산 경로.
    // 체크박스가 0개여도 유효한 선택이라 그대로 저장된다(ComboSimulationService가 baseline을 돌려줌).
    @PostMapping({"/plan", "/save-plan"})
    public ResponseEntity<PlanSaveResponse> savePlan(
            @RequestBody PlanSaveRequest request, Authentication authentication, HttpSession session
    ) {
        Long userId = currentUser.getUserId(authentication);
        List<LeverIntensityCalculator.LeverSelection> selections = request.levers() == null
                ? List.of()
                : request.levers().stream()
                        .map(entry -> new LeverIntensityCalculator.LeverSelection(entry.type(), entry.intensity()))
                        .toList();

        ComboSimulationService.ComboResult result = comboSimulationService.simulate(userId, request.goalAmount(), selections, request.assumedReturnRate());
        BigDecimal finalNetWorth = result.timeline().get(result.timeline().size() - 1).netWorth();

        // baseline_months_to_goal/projected_months_to_goal은 NOT NULL 컬럼인데 엔진은 "도달 불가"를 null로
        // 표현한다 — 시뮬레이션 상한(PROJECTION_UNREACHABLE_SENTINEL_MONTHS, 20년=240개월)으로 저장해서
        // "이 계획대로면 20년 안에 도달 못 함"을 나타낸다.
        int baselineToStore = result.baselineMonthsToGoal() != null
                ? result.baselineMonthsToGoal() : PROJECTION_UNREACHABLE_SENTINEL_MONTHS;
        int projectedToStore = result.comboMonthsToGoal() != null
                ? result.comboMonthsToGoal() : PROJECTION_UNREACHABLE_SENTINEL_MONTHS;

        int diffToStore = result.diffMonths() != null ? result.diffMonths() : 0;
        String loanImpactJson = buildLoanImpactJson(userId, selections);
        BenchmarkInfo benchmarkInfo = resolveBenchmarkInfo(userId, session);
        String benchmarkType = (String) session.getAttribute(FutureSimViewController.COMPARISON_BASIS_SESSION_KEY);
        String savedPlanName = planSnapshotService.save(
                userId, request.planName(), request.goalAmount(), request.goalPresetKey(), benchmarkType,
                benchmarkInfo.label(), benchmarkInfo.medianNetWorth(),
                request.assumedReturnRate() == null ? appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE") : request.assumedReturnRate(), selections,
                baselineToStore, projectedToStore, diffToStore, loanImpactJson, finalNetWorth
        );

        return ResponseEntity.ok(new PlanSaveResponse(savedPlanName, result.baselineMonthsToGoal(), result.comboMonthsToGoal(), finalNetWorth));
    }

    // 저장된 계획 목록(별도 페이지)에서 쓴다 — 이름/목표금액/앞당긴기간/수정시각만 내려주고,
    // 불러오기는 FutureSimViewController가 서버에서 바로 세션에 반영하는 방식이라 여기엔 없다.
    @GetMapping("/plans")
    public ResponseEntity<List<PlanSummaryResponse>> plans(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        List<PlanSummaryResponse> summaries = planSnapshotService.list(userId).stream()
                .map(plan -> new PlanSummaryResponse(
                        plan.getId(), plan.getPlanName(), plan.getGoalAmount(),
                        plan.getProjectedMonthsToGoal(), plan.getDiffMonths(), plan.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    private String buildLoanImpactJson(Long userId, List<LeverIntensityCalculator.LeverSelection> selections) {
        if (selections.stream().noneMatch(item -> item.leverType() == LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT
                || item.leverType() == LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION)) return "[]";
        var combined = leverLoanComparisonService.combinedImpact(userId, selections);
        if (combined == null) return "[]";
        return "[{\"loanId\":" + combined.loanId()
                + ",\"loanType\":\"" + combined.loanType() + "\""
                + ",\"beforeMonthlyPayment\":" + combined.beforeMonthlyPayment()
                + ",\"afterMonthlyPayment\":" + combined.afterMonthlyPayment()
                + ",\"totalInterestDiff\":" + combined.totalInterestDiff()
                + ",\"beforeRemainingMonths\":" + combined.beforeRemainingMonths()
                + ",\"afterRemainingMonths\":" + combined.afterRemainingMonths()
                + ",\"prepaymentFeeRate\":" + nullableNumber(combined.prepaymentFeeRate())
                + ",\"prepaymentFeeEndDate\":" + (combined.prepaymentFeeEndDate() == null ? "null" : "\"" + combined.prepaymentFeeEndDate() + "\"")
                + "}]";
    }

    private String nullableNumber(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }
}
