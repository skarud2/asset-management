package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.service.LiquidAssetAggregator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 4단계에서 계산하고 5단계의 기본 계획으로 넘기는 실행 가능한 추천 조합. */
@Service
public class RecommendedComboService {
    private static final String RETURN_RATE_KEY = "FUTURESIM_ASSUMED_RETURN_RATE";
    private final LeverIntensityCalculator leverCalculator;
    private final FutureSimulationEngine engine;
    private final LiquidAssetAggregator liquidAssetAggregator;
    private final AppConfigService appConfigService;

    public RecommendedComboService(LeverIntensityCalculator leverCalculator, FutureSimulationEngine engine,
                                   LiquidAssetAggregator liquidAssetAggregator, AppConfigService appConfigService) {
        this.leverCalculator = leverCalculator;
        this.engine = engine;
        this.liquidAssetAggregator = liquidAssetAggregator;
        this.appConfigService = appConfigService;
    }

    public record RecommendedLever(LeverIntensityCalculator.LeverType type, BigDecimal intensity,
                                   Integer diffMonths, boolean assumption) {}
    public record RecommendedCombo(List<RecommendedLever> levers, Integer baselineMonthsToGoal,
                                   Integer comboMonthsToGoal, Integer diffMonths, BigDecimal assumedReturnRate) {}

    public RecommendedCombo getRecommendedCombo(Long userId, BigDecimal goalAmount, BigDecimal monthlyExtraCapacity,
                                                BigDecimal prepaymentAmount, BigDecimal termExtensionMonths) {
        BigDecimal rate = appConfigService.getDecimal(RETURN_RATE_KEY);
        BigDecimal liquidAsset = liquidAssetAggregator.aggregate(userId).totalLiquidAssets();
        List<RecommendedLever> candidates = new ArrayList<>();
        for (LeverIntensityCalculator.LeverType type : LeverIntensityCalculator.LeverType.values()) {
            if (type == LeverIntensityCalculator.LeverType.NEW_LOAN || !leverCalculator.isLeverAvailable(userId, type)) continue;
            BigDecimal intensity = recommendedIntensity(type, monthlyExtraCapacity, prepaymentAmount, termExtensionMonths);
            if (intensity == null) continue;
            Integer diff = leverCalculator.calculateDiffMonths(userId, goalAmount, type, intensity, rate);
            if (diff == null || diff <= 0) continue;
            if (type == LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT
                    && (liquidAsset == null || liquidAsset.compareTo(intensity) < 0)) continue;
            candidates.add(new RecommendedLever(type, intensity, diff, type == LeverIntensityCalculator.LeverType.INCOME_CHANGE));
        }
        List<LeverIntensityCalculator.LeverSelection> selections = candidates.stream()
                .map(item -> new LeverIntensityCalculator.LeverSelection(item.type(), item.intensity())).toList();
        Integer baseline = engine.calculateMonthsToGoalCompound(userId, goalAmount, FutureSimulationEngine.Adjustment.NONE, rate);
        Integer combo = engine.calculateMonthsToGoalCompound(userId, goalAmount,
                leverCalculator.resolveCombinedAdjustment(userId, selections), rate);
        Integer diff = baseline == null || combo == null ? null : baseline - combo;
        return new RecommendedCombo(candidates, baseline, combo, diff, rate);
    }

    public RecommendedCombo getRecommendedCombo(Long userId, BigDecimal goalAmount) {
        return getRecommendedCombo(userId, goalAmount, null, null, null);
    }

    // 4단계 레버 카드에서 사용자가 고른 강도(monthlyExtraCapacity/prepaymentAmount/termExtensionMonths)를
    // 그대로 쓰고, 아직 카드를 건드리지 않은 레버만 기본값으로 채운다.
    private BigDecimal recommendedIntensity(
            LeverIntensityCalculator.LeverType type, BigDecimal monthlyExtraCapacity,
            BigDecimal prepaymentAmount, BigDecimal termExtensionMonths
    ) {
        return switch (type) {
            case INCOME_CHANGE -> monthlyExtraCapacity != null && monthlyExtraCapacity.signum() > 0
                    ? monthlyExtraCapacity : LeverIntensityCalculator.DEFAULT_MONTHLY_EXTRA_CAPACITY;
            case LOAN_PREPAYMENT -> prepaymentAmount != null && prepaymentAmount.signum() > 0
                    ? prepaymentAmount : LeverIntensityCalculator.DEFAULT_PREPAYMENT_AMOUNT;
            case LOAN_TERM_EXTENSION -> termExtensionMonths != null && termExtensionMonths.signum() > 0
                    ? termExtensionMonths : LeverIntensityCalculator.DEFAULT_TERM_EXTENSION_MONTHS;
            case NEW_LOAN -> null;
        };
    }
}
