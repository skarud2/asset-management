package com.via.shinvia.report.service;

import com.via.shinvia.futuresim.service.AppConfigService;
import com.via.shinvia.futuresim.service.FutureSimulationEngine;
import com.via.shinvia.futuresim.service.UserFinancialSnapshotService;
import com.via.shinvia.report.mapper.ReportSpendingMapper;
import com.via.shinvia.stresstest.dto.request.StressTestRequest;
import com.via.shinvia.stresstest.dto.response.StressTestResponse;
import com.via.shinvia.stresstest.service.PersonalStressTestSimulator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FinancialScoreCalculator {

    private static final BigDecimal DSR_ZERO_SCORE_AT_PERCENT = new BigDecimal("40");
    private static final BigDecimal SAVINGS_RATE_HUNDRED_SCORE_AT_PERCENT = new BigDecimal("30");
    private static final BigDecimal DEBT_RATIO_ZERO_SCORE_AT_PERCENT = new BigDecimal("200");
    private static final int RUNWAY_HUNDRED_SCORE_AT_MONTHS = 12;
    private static final double SPENDING_CV_ZERO_SCORE_AT = 0.5;
    private static final BigDecimal NEUTRAL_SCORE = new BigDecimal("50");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal STRESS_RATE_DELTA_PERCENT = new BigDecimal("1.0");
    private static final BigDecimal STRESS_INCOME_DROP_PERCENT = new BigDecimal("20");
    private static final BigDecimal STRESS_UNEXPECTED_EXPENSE = BigDecimal.ZERO;
    private static final int STRESS_SIMULATION_MONTHS = 24;

    private static final int SPENDING_LOOKBACK_MONTHS = 6;

    private final UserFinancialSnapshotService userFinancialSnapshotService;
    private final FutureSimulationEngine futureSimulationEngine;
    private final PersonalStressTestSimulator personalStressTestSimulator;
    private final ReportSpendingMapper reportSpendingMapper;
    private final AppConfigService appConfigService;

    public FinancialScoreCalculator(
            UserFinancialSnapshotService userFinancialSnapshotService,
            FutureSimulationEngine futureSimulationEngine,
            PersonalStressTestSimulator personalStressTestSimulator,
            ReportSpendingMapper reportSpendingMapper,
            AppConfigService appConfigService
    ) {
        this.userFinancialSnapshotService = userFinancialSnapshotService;
        this.futureSimulationEngine = futureSimulationEngine;
        this.personalStressTestSimulator = personalStressTestSimulator;
        this.reportSpendingMapper = reportSpendingMapper;
        this.appConfigService = appConfigService;
    }

    public Result calculate(Long userId) {
        UserFinancialSnapshotService.Snapshot snapshot = userFinancialSnapshotService.getSnapshot(userId);

        BigDecimal dsrWeight = appConfigService.getDecimal("REPORT_SCORE_WEIGHT_DSR");
        BigDecimal savingsRateWeight = appConfigService.getDecimal("REPORT_SCORE_WEIGHT_SAVINGS_RATE");
        BigDecimal debtRatioWeight = appConfigService.getDecimal("REPORT_SCORE_WEIGHT_DEBT_RATIO");
        BigDecimal vulnerabilityWeight = appConfigService.getDecimal("REPORT_SCORE_WEIGHT_VULNERABILITY");
        BigDecimal spendingStabilityWeight = appConfigService.getDecimal("REPORT_SCORE_WEIGHT_SPENDING_STABILITY");

        List<DimensionScore> dimensions = List.of(
                new DimensionScore("DSR", "DSR건전성", dsrHealthScore(snapshot), dsrWeight),
                new DimensionScore("SAVINGS_RATE", "저축률", savingsRateScore(userId), savingsRateWeight),
                new DimensionScore("DEBT_RATIO", "부채비율", debtRatioScore(snapshot), debtRatioWeight),
                new DimensionScore("VULNERABILITY", "취약도역산", vulnerabilityScore(userId), vulnerabilityWeight),
                new DimensionScore("SPENDING_STABILITY", "소비안정성", spendingStabilityScore(userId), spendingStabilityWeight)
        );

        BigDecimal weightSum = dimensions.stream().map(DimensionScore::weightPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedSum = dimensions.stream()
                .map(d -> d.score().multiply(d.weightPercent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalScore = weightSum.signum() == 0
                ? NEUTRAL_SCORE
                : weightedSum.divide(weightSum, 1, RoundingMode.HALF_UP);

        return new Result(totalScore, dimensions);
    }

    private BigDecimal dsrHealthScore(UserFinancialSnapshotService.Snapshot snapshot) {
        if (snapshot.annualIncome() == null || snapshot.annualIncome().signum() <= 0) {
            return NEUTRAL_SCORE;
        }
        BigDecimal dsrPercent = snapshot.annualDebtRepayment()
                .divide(snapshot.annualIncome(), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        return scoreDescending(dsrPercent, DSR_ZERO_SCORE_AT_PERCENT);
    }

    private BigDecimal savingsRateScore(Long userId) {
        FutureSimulationEngine.SavingsCapacity capacity = futureSimulationEngine.calculateSavingsCapacity(userId);
        if (capacity.monthlyIncome() == null || capacity.monthlyIncome().signum() <= 0) {
            return NEUTRAL_SCORE;
        }
        BigDecimal ratePercent = capacity.monthlySavingsCapacity()
                .divide(capacity.monthlyIncome(), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        return scoreAscending(ratePercent, SAVINGS_RATE_HUNDRED_SCORE_AT_PERCENT);
    }

    private BigDecimal debtRatioScore(UserFinancialSnapshotService.Snapshot snapshot) {
        BigDecimal denominator = null;
        if (snapshot.liquidAsset() != null && snapshot.liquidAsset().signum() > 0) {
            denominator = snapshot.liquidAsset();
        } else if (snapshot.annualIncome() != null && snapshot.annualIncome().signum() > 0) {
            denominator = snapshot.annualIncome();
        }
        if (denominator == null || snapshot.totalDebt() == null) {
            return NEUTRAL_SCORE;
        }
        BigDecimal ratioPercent = snapshot.totalDebt()
                .divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        return scoreDescending(ratioPercent, DEBT_RATIO_ZERO_SCORE_AT_PERCENT);
    }

    private BigDecimal vulnerabilityScore(Long userId) {
        StressTestResponse response = personalStressTestSimulator.simulate(
                new StressTestRequest(userId, STRESS_RATE_DELTA_PERCENT, STRESS_INCOME_DROP_PERCENT,
                        STRESS_UNEXPECTED_EXPENSE, STRESS_SIMULATION_MONTHS)
        );
        if (!response.runwayCalculable() || response.runwayMonths() == null) {
            return NEUTRAL_SCORE;
        }
        return scoreAscending(BigDecimal.valueOf(response.runwayMonths()), BigDecimal.valueOf(RUNWAY_HUNDRED_SCORE_AT_MONTHS));
    }

    private BigDecimal spendingStabilityScore(Long userId) {
        List<BigDecimal> monthlyTotals = reportSpendingMapper.findMonthlyTotalsByUserId(userId, SPENDING_LOOKBACK_MONTHS);
        if (monthlyTotals == null || monthlyTotals.size() < 2) {
            return NEUTRAL_SCORE;
        }
        double mean = monthlyTotals.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (mean <= 0) {
            return NEUTRAL_SCORE;
        }
        double variance = monthlyTotals.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                .average().orElse(0);
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return scoreDescending(BigDecimal.valueOf(coefficientOfVariation), BigDecimal.valueOf(SPENDING_CV_ZERO_SCORE_AT));
    }

    private BigDecimal scoreDescending(BigDecimal value, BigDecimal zeroScoreAt) {
        if (value.signum() <= 0) return HUNDRED;
        if (value.compareTo(zeroScoreAt) >= 0) return BigDecimal.ZERO;
        return HUNDRED.subtract(value.divide(zeroScoreAt, 4, RoundingMode.HALF_UP).multiply(HUNDRED))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreAscending(BigDecimal value, BigDecimal hundredScoreAt) {
        if (value.signum() <= 0) return BigDecimal.ZERO;
        if (value.compareTo(hundredScoreAt) >= 0) return HUNDRED;
        return value.divide(hundredScoreAt, 4, RoundingMode.HALF_UP).multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_UP);
    }

    public record Result(BigDecimal totalScore, List<DimensionScore> dimensions) {
    }

    public record DimensionScore(String key, String label, BigDecimal score, BigDecimal weightPercent) {
    }
}
