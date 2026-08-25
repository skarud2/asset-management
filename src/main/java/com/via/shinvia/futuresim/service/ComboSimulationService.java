package com.via.shinvia.futuresim.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// 5단계("레버 조합해보기")용 — 선택된 레버 여러 개를 한 번에 FutureSimulationEngine에 투입해서
// 기준선(레버 없음) 대비 조합 실행 결과를 비교한다. 레버 조합 자체(상호작용 포함)는
// LeverIntensityCalculator.resolveCombinedAdjustment()에 위임한다.
@Service
public class ComboSimulationService {

    private final FutureSimulationEngine engine;
    private final LeverIntensityCalculator leverCalculator;

    public ComboSimulationService(FutureSimulationEngine engine, LeverIntensityCalculator leverCalculator) {
        this.engine = engine;
        this.leverCalculator = leverCalculator;
    }

    public record ComboResult(
            Integer baselineMonthsToGoal,
            Integer comboMonthsToGoal,
            Integer diffMonths,
            List<FutureSimulationEngine.TimelinePoint> timeline
    ) {
    }

    public ComboResult simulate(Long userId, BigDecimal goalAmount, List<LeverIntensityCalculator.LeverSelection> selections) {
        return simulate(userId, goalAmount, selections, null);
    }
    public ComboResult simulate(Long userId, BigDecimal goalAmount, List<LeverIntensityCalculator.LeverSelection> selections, BigDecimal rate) {
        FutureSimulationEngine.Projection baseline = rate == null ? engine.calculateProjection(userId, goalAmount) : engine.calculateProjection(userId, goalAmount, rate);

        // 레버를 하나도 안 고른 것도 유효한 선택이라, baseline 그 자체를 "조합 결과"로 그대로 돌려준다.
        if (selections == null || selections.isEmpty()) {
            return new ComboResult(baseline.monthsToGoal(), baseline.monthsToGoal(), 0, baseline.timeline());
        }

        FutureSimulationEngine.Adjustment adjustment = leverCalculator.resolveCombinedAdjustment(userId, selections);
        FutureSimulationEngine.Projection combo = rate == null ? engine.calculateProjection(userId, goalAmount, adjustment) : engine.calculateProjection(userId, goalAmount, adjustment, rate);

        Integer diffMonths = (baseline.monthsToGoal() == null || combo.monthsToGoal() == null)
                ? null
                : baseline.monthsToGoal() - combo.monthsToGoal();

        return new ComboResult(baseline.monthsToGoal(), combo.monthsToGoal(), diffMonths, combo.timeline());
    }
}
