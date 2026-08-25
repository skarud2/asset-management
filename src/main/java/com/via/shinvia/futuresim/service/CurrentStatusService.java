package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.entity.FutureSimUserProfile;
import com.via.shinvia.futuresim.mapper.FutureSimUserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

// 미래 금융 시뮬레이터 1단계("지금 내 상태") 화면에 필요한 데이터를 한 번에 모아준다.
// - 연령대 패널: app_user.birth_date로 나이 계산 → HouseholdNetWorthBenchmarkService.getAgeGroupBenchmark()
// - 가구원별 패널: app_user.household_size → HouseholdNetWorthBenchmarkService.getBenchmark()
// - 내 실제 소득/자산/부채/순자산: UserFinancialSnapshotService
@Service
public class CurrentStatusService {

    private final FutureSimUserProfileMapper userProfileMapper;
    private final HouseholdNetWorthBenchmarkService benchmarkService;
    private final UserFinancialSnapshotService snapshotService;

    public CurrentStatusService(
            FutureSimUserProfileMapper userProfileMapper,
            HouseholdNetWorthBenchmarkService benchmarkService,
            UserFinancialSnapshotService snapshotService
    ) {
        this.userProfileMapper = userProfileMapper;
        this.benchmarkService = benchmarkService;
        this.snapshotService = snapshotService;
    }

    // sessionHouseholdSize: 이 시뮬레이션 안에서만 임시로 골라본 가구원수(세션 값, DB 미저장).
    // app_user.household_size에 저장된 값보다 우선한다 — "이번 시뮬레이션에서만 잠깐 바꿔보는" 용도라
    // 프로필 자체를 덮어쓰지 않는다.
    public CurrentStatusView getCurrentStatus(Long userId, String sessionHouseholdSize) {
        FutureSimUserProfile profile = userProfileMapper.findByUserId(userId);
        UserFinancialSnapshotService.Snapshot mySnapshot = snapshotService.getSnapshot(userId);

        AgePanel agePanel = buildAgePanel(profile);

        String profileHouseholdSize = (profile == null) ? null : profile.getHouseholdSize();
        String householdSize = (sessionHouseholdSize != null) ? sessionHouseholdSize : profileHouseholdSize;
        HouseholdNetWorthBenchmarkService.Result householdBenchmark = benchmarkService.getBenchmark(householdSize);

        return new CurrentStatusView(mySnapshot, agePanel, householdBenchmark);
    }

    private AgePanel buildAgePanel(FutureSimUserProfile profile) {
        if (profile == null || profile.getBirthDate() == null) {
            return AgePanel.unavailable();
        }

        int userAge = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        HouseholdNetWorthBenchmarkService.AgeGroupResult benchmark = benchmarkService.getAgeGroupBenchmark(userAge);

        return AgePanel.available(userAge, benchmark);
    }

    public record CurrentStatusView(
            UserFinancialSnapshotService.Snapshot mySnapshot,
            AgePanel agePanel,
            HouseholdNetWorthBenchmarkService.Result householdBenchmark
    ) {
    }

    public record AgePanel(
            boolean available,
            Integer userAge,
            HouseholdNetWorthBenchmarkService.AgeGroupResult benchmark
    ) {
        static AgePanel unavailable() {
            return new AgePanel(false, null, null);
        }

        static AgePanel available(int userAge, HouseholdNetWorthBenchmarkService.AgeGroupResult benchmark) {
            return new AgePanel(true, userAge, benchmark);
        }
    }
}
