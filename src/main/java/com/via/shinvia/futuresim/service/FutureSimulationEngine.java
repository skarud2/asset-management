package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.service.LivingExpenseEstimator;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.via.shinvia.futuresim.mapper.FutureSimUserProfileMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.Period;

// 목표 금액까지 걸리는 개월수를 "이벤트 없는 기본 실행"으로 계산한다.
// 매달 (소득 - 생활비 - 대출상환액)만큼 순자산이 늘어난다고 가정하는 단순 선형 모델 —
// 나중에 결혼/출산/이직 같은 생애 이벤트를 반영하는 시뮬레이션이 생기면
// calculateSavingsCapacity()를 그대로 재사용하고 이벤트에 따른 조정만 얹으면 된다.
@Service
public class FutureSimulationEngine {

    private static final int LIVING_EXPENSE_LOOKBACK_MONTHS = 3;
    private static final int MAX_MONTHS = 1200; // 100년 초과분은 "도달 불가"로 취급

    // 사회 초년생처럼 목표까지 20년 이상 걸리는 사용자도 도달 시점을 보여줄 수 있도록,
    // 3·4·5단계의 장기 예측 상한을 calculateMonthsToGoal()와 같은 100년으로 둔다.
    private static final String ASSUMED_RETURN_RATE_CONFIG_KEY = "FUTURESIM_ASSUMED_RETURN_RATE";
    private static final int PROJECTION_MAX_MONTHS = 1200;
    private static final int PROJECTION_POST_GOAL_BUFFER_MONTHS = 12;

    private final UserFinancialSnapshotService snapshotService;
    private final LivingExpenseEstimator livingExpenseEstimator;
    private final AppConfigService appConfigService;
    private final FutureSimUserProfileMapper profileMapper;

    public FutureSimulationEngine(
            UserFinancialSnapshotService snapshotService,
            LivingExpenseEstimator livingExpenseEstimator,
            AppConfigService appConfigService
    ) {
        this(snapshotService, livingExpenseEstimator, appConfigService, null);
    }

    @Autowired
    public FutureSimulationEngine(
            UserFinancialSnapshotService snapshotService,
            LivingExpenseEstimator livingExpenseEstimator,
            AppConfigService appConfigService,
            FutureSimUserProfileMapper profileMapper
    ) {
        this.snapshotService = snapshotService;
        this.livingExpenseEstimator = livingExpenseEstimator;
        this.appConfigService = appConfigService;
        this.profileMapper = profileMapper;
    }

    // 이미 목표를 달성했으면 0, 저축 여력이 0 이하이거나 100년을 넘게 걸리면 도달 불가(null)를 반환한다.
    public Integer calculateMonthsToGoal(Long userId, BigDecimal goalAmount) {
        UserFinancialSnapshotService.Snapshot snapshot = snapshotService.getSnapshot(userId);
        BigDecimal currentNetWorth = snapshot.netWorth() != null ? snapshot.netWorth() : BigDecimal.ZERO;

        if (currentNetWorth.compareTo(goalAmount) >= 0) {
            return 0;
        }

        BigDecimal monthlyNetSavings = calculateSavingsCapacity(userId, snapshot).monthlySavingsCapacity();
        if (monthlyNetSavings.signum() <= 0) {
            return null;
        }

        BigDecimal remaining = goalAmount.subtract(currentNetWorth);
        BigDecimal months = remaining.divide(monthlyNetSavings, 0, RoundingMode.CEILING);

        return months.compareTo(BigDecimal.valueOf(retirementHorizonMonths(userId))) > 0 ? null : months.intValue();
    }

    // 목표 화면과 향후 이벤트 시뮬레이션이 함께 쓰는 현금흐름 기준값.
    // 소득은 기존 금융 프로필, 생활비는 MyData 카드 거래 최근 3개월 평균,
    // 상환액은 실제 보유 대출의 원리금 상환 스케줄 합계(LoanBurdenAggregator)다.
    public SavingsCapacity calculateSavingsCapacity(Long userId) {
        return calculateSavingsCapacity(userId, snapshotService.getSnapshot(userId));
    }

    private SavingsCapacity calculateSavingsCapacity(Long userId, UserFinancialSnapshotService.Snapshot snapshot) {
        BigDecimal monthlyIncome = snapshot.annualIncome() != null
                ? snapshot.annualIncome().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LivingExpenseEstimator.Result livingExpense =
                livingExpenseEstimator.estimate(userId, LIVING_EXPENSE_LOOKBACK_MONTHS);
        BigDecimal monthlyExpense = livingExpense.monthlyLivingExpense();

        BigDecimal monthlyLoanPayment = snapshot.monthlyLoanPayment() != null
                ? snapshot.monthlyLoanPayment()
                : BigDecimal.ZERO;

        BigDecimal capacity = monthlyIncome.subtract(monthlyExpense).subtract(monthlyLoanPayment);
        return new SavingsCapacity(monthlyIncome, monthlyExpense, monthlyLoanPayment, capacity);
    }

    public record SavingsCapacity(
            BigDecimal monthlyIncome,
            BigDecimal monthlyLivingExpense,
            BigDecimal monthlyLoanPayment,
            BigDecimal monthlySavingsCapacity
    ) {
        public boolean hasSavingsCapacity() {
            return monthlySavingsCapacity.signum() > 0;
        }
    }

    // 3단계("성장 곡선")용 월별 순자산 경로. calculateMonthsToGoal()과 달리 닫힌 형태 나눗셈이 아니라
    // 매달 직접 굴려서(compound) 타임라인을 만든다 — 그래프에 그대로 찍을 좌표가 필요하기 때문.
    //
    // 대출 잔액(totalDebt)은 이 시뮬레이션 동안 고정값으로 둔다. 실제 대출 상환에 따른 잔액 감소는
    // loan/ 쪽 상환 스케줄 계산(LoanRepaymentCalculator) 영역이라 여기서는 건드리지 않고,
    // 유동자산(liquidAssets)만 "여유자금 평균 수익률" 가정으로 복리 성장시킨다:
    //   liquidAssets = liquidAssets × (1 + monthlyRate) + 월 순현금흐름
    //   netWorth(t) = liquidAssets(t) − totalDebt
    public Projection calculateProjection(Long userId, BigDecimal goalAmount) {
        return calculateProjection(userId, goalAmount, Adjustment.NONE);
    }

    public Projection calculateProjection(Long userId, BigDecimal goalAmount, BigDecimal annualReturnRatePercent) {
        return calculateProjection(userId, goalAmount, Adjustment.NONE, annualReturnRatePercent);
    }

    // 5단계("레버 조합해보기")용 — calculateProjection()과 같은 타임라인을 만들되, 레버 조합으로 만든
    // Adjustment를 시작 시점(liquidAssets/totalDebt/월현금흐름)에 얹은 채로 굴린다. 3단계가 쓰는
    // calculateProjection(userId, goalAmount)는 이 메서드를 Adjustment.NONE으로 호출하는 것과 동일해서
    // 기존 동작은 그대로다.
    public Projection calculateProjection(Long userId, BigDecimal goalAmount, Adjustment adjustment) {
        return calculateProjectionInternal(userId, goalAmount, adjustment, appConfigService.getDecimal(ASSUMED_RETURN_RATE_CONFIG_KEY));
    }

    public Projection calculateProjection(Long userId, BigDecimal goalAmount, Adjustment adjustment, BigDecimal annualReturnRatePercent) {
        return calculateProjectionInternal(userId, goalAmount, adjustment, annualReturnRatePercent);
    }

    private Projection calculateProjectionInternal(Long userId, BigDecimal goalAmount, Adjustment adjustment, BigDecimal annualReturnRatePercent) {
        UserFinancialSnapshotService.Snapshot snapshot = snapshotService.getSnapshot(userId);
        BigDecimal totalDebt = (snapshot.totalDebt() != null ? snapshot.totalDebt() : BigDecimal.ZERO)
                .add(adjustment.totalDebtDelta());
        SavingsCapacity savingsCapacity = calculateSavingsCapacity(userId, snapshot);
        BigDecimal monthlyNetCashFlow = savingsCapacity.monthlySavingsCapacity().add(adjustment.monthlyCashFlowDelta());
        BigDecimal monthlyRate = toMonthlyRate(annualReturnRatePercent);

        BigDecimal liquidAssets = (snapshot.liquidAsset() != null ? snapshot.liquidAsset() : BigDecimal.ZERO)
                .add(adjustment.liquidAssetsDelta());
        BigDecimal startNetWorth = liquidAssets.subtract(totalDebt);

        List<TimelinePoint> timeline = new ArrayList<>();
        timeline.add(toTimelinePoint(0, startNetWorth, startNetWorth, monthlyNetCashFlow));

        Integer monthsToGoal = startNetWorth.compareTo(goalAmount) >= 0 ? 0 : null;

        for (int month = 1; month <= retirementHorizonMonths(userId); month++) {
            liquidAssets = compound(liquidAssets, monthlyRate, monthlyNetCashFlow);
            BigDecimal netWorth = liquidAssets.subtract(totalDebt);
            timeline.add(toTimelinePoint(month, netWorth, startNetWorth, monthlyNetCashFlow));

            if (monthsToGoal == null && netWorth.compareTo(goalAmount) >= 0) {
                monthsToGoal = month;
            }
            if (monthsToGoal != null && month >= monthsToGoal + PROJECTION_POST_GOAL_BUFFER_MONTHS) {
                break;
            }
        }

        return new Projection(monthsToGoal, timeline, goalAmount, monthsToGoal != null, savingsCapacity, annualReturnRatePercent, totalDebt);
    }

    // 누적구성 영역 차트용 — 순자산을 "내가 직접 모은 돈"(원금, 복리 없이 단순 누적)과
    // "투자수익"(복리 효과로 원금 위에 더 붙은 부분)으로 쪼갠다.
    private TimelinePoint toTimelinePoint(int monthOffset, BigDecimal netWorth, BigDecimal startNetWorth, BigDecimal monthlyNetCashFlow) {
        BigDecimal linearContribution = startNetWorth.add(monthlyNetCashFlow.multiply(BigDecimal.valueOf(monthOffset)));
        BigDecimal contributionAmount = linearContribution.min(netWorth);
        BigDecimal returnAmount = netWorth.subtract(contributionAmount).max(BigDecimal.ZERO);
        return new TimelinePoint(monthOffset, netWorth, contributionAmount, returnAmount);
    }

    private BigDecimal toMonthlyRate(BigDecimal annualReturnRatePercent) {
        return annualReturnRatePercent
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
    }

    // liquidAssets × (1 + monthlyRate) + 월현금흐름 한 스텝. BigDecimal#multiply는 두 피연산자의 scale을
    // 더한 결과를 돌려주기 때문에(여기선 매 스텝 스케일이 10씩 늘어남), 반올림 없이 240번 반복하면
    // 소수점이 수백 자리까지 불어난다 — 매 스텝 끝에 돈 단위 정밀도(소수 2자리)로 재반올림해 원천 차단한다.
    private BigDecimal compound(BigDecimal liquidAssets, BigDecimal monthlyRate, BigDecimal monthlyNetCashFlow) {
        return liquidAssets.multiply(BigDecimal.ONE.add(monthlyRate))
                .add(monthlyNetCashFlow)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // 4단계("레버 랭킹")용 — liquidAssets/totalDebt/월현금흐름에 조정값을 얹어서 monthsToGoal만 빠르게 계산한다.
    // calculateProjection()과 같은 복리 루프를 쓰지만, 레버당 8~10번씩 도는 경로라 타임라인 전체를
    // 만들지 않고 monthsToGoal만 반환하는 가벼운 버전으로 따로 둔다.
    public Integer calculateMonthsToGoalCompound(Long userId, BigDecimal goalAmount, Adjustment adjustment) {
        return calculateMonthsToGoalCompound(userId, goalAmount, adjustment, appConfigService.getDecimal(ASSUMED_RETURN_RATE_CONFIG_KEY));
    }

    public Integer calculateMonthsToGoalCompound(Long userId, BigDecimal goalAmount, Adjustment adjustment, BigDecimal annualReturnRatePercent) {
        UserFinancialSnapshotService.Snapshot snapshot = snapshotService.getSnapshot(userId);
        BigDecimal totalDebt = (snapshot.totalDebt() != null ? snapshot.totalDebt() : BigDecimal.ZERO)
                .add(adjustment.totalDebtDelta());
        SavingsCapacity savingsCapacity = calculateSavingsCapacity(userId, snapshot);
        BigDecimal monthlyNetCashFlow = savingsCapacity.monthlySavingsCapacity().add(adjustment.monthlyCashFlowDelta());
        BigDecimal monthlyRate = toMonthlyRate(annualReturnRatePercent);

        BigDecimal liquidAssets = (snapshot.liquidAsset() != null ? snapshot.liquidAsset() : BigDecimal.ZERO)
                .add(adjustment.liquidAssetsDelta());
        BigDecimal netWorth = liquidAssets.subtract(totalDebt);

        if (netWorth.compareTo(goalAmount) >= 0) {
            return 0;
        }

        for (int month = 1; month <= retirementHorizonMonths(userId); month++) {
            liquidAssets = compound(liquidAssets, monthlyRate, monthlyNetCashFlow);
            netWorth = liquidAssets.subtract(totalDebt);
            if (netWorth.compareTo(goalAmount) >= 0) {
                return month;
            }
        }
        return null;
    }

    // 레버 계산에서 baseline(조정 없음) 대비 효과를 재는 데 쓰는, 순자산 시작값/월현금흐름에 대한 가감값.
    public record Adjustment(
            BigDecimal liquidAssetsDelta,
            BigDecimal totalDebtDelta,
            BigDecimal monthlyCashFlowDelta
    ) {
        public static final Adjustment NONE = new Adjustment(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public record TimelinePoint(
            int monthOffset,
            BigDecimal netWorth,
            BigDecimal contributionAmount,
            BigDecimal returnAmount
    ) {
    }

    public record Projection(
            Integer monthsToGoal,
            List<TimelinePoint> timeline,
            BigDecimal goalAmount,
            boolean crossoverReached,
            SavingsCapacity savingsCapacity,
            BigDecimal assumedReturnRatePercent,
            BigDecimal totalDebt
    ) {
    }

    private int retirementHorizonMonths(Long userId) {
        if (profileMapper == null) return PROJECTION_MAX_MONTHS;
        var profile = profileMapper.findByUserId(userId);
        if (profile == null || profile.getBirthDate() == null) return PROJECTION_MAX_MONTHS;
        int years = Math.max(0, 60 - Period.between(profile.getBirthDate(), LocalDate.now()).getYears());
        return Math.max(1, years * 12);
    }
}
