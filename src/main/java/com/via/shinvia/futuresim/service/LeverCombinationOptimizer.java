package com.via.shinvia.futuresim.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 레버 4종을 "따로따로 얼마씩 당겨지나"가 아니라 "다 같이 쓰면 어떤 조합이 최선인가"로 묻는 질문에 답한다.
// 레버 그룹(소득변화/조기상환/만기연장/신규대출)마다 스킵을 포함한 몇 개의 선택지가 있고, 그 중 조기상환만
// 실제 자원(유동자산)을 소비한다 — 즉 조기상환 강도의 합이 사용자의 유동자산을 넘을 수 없다는 제약 하나만
// 존재하는 multiple-choice knapsack 문제다. 그룹을 순서대로 결정해나가는 재귀 + 메모이제이션(top-down DP)으로
// 풀고, 각 리프(조합 하나)는 FutureSimulationEngine으로 딱 한 번만 계산해서 캐싱한다 — CLAUDE.md가 명시한
// "상태 수가 크므로 메모이제이션/캐싱 전략 필수" 원칙을 그대로 따른다.
@Service
public class LeverCombinationOptimizer {

    private final FutureSimulationEngine engine;
    private final LeverIntensityCalculator leverCalculator;
    private final UserFinancialSnapshotService snapshotService;

    public LeverCombinationOptimizer(
            FutureSimulationEngine engine,
            LeverIntensityCalculator leverCalculator,
            UserFinancialSnapshotService snapshotService
    ) {
        this.engine = engine;
        this.leverCalculator = leverCalculator;
        this.snapshotService = snapshotService;
    }

    // 레버 그룹 하나 안에서 고를 수 있는 선택지 하나. "스킵"도 선택지 중 하나(효과·비용 없음)로 취급한다.
    public record LeverChoice(LeverIntensityCalculator.LeverType leverType, BigDecimal intensity, boolean skipped) {
    }

    public record CombinationResult(
            List<LeverChoice> chosenLevers,
            Integer baselineMonths,
            Integer combinedMonths,
            Integer diffMonths,
            BigDecimal liquidAssetBudget,
            int combinationsEvaluated
    ) {
    }

    private record Choice(LeverIntensityCalculator.LeverType leverType, BigDecimal intensity, boolean skipped,
                           FutureSimulationEngine.Adjustment adjustment) {
        static Choice skip(LeverIntensityCalculator.LeverType leverType) {
            return new Choice(leverType, BigDecimal.ZERO, true, FutureSimulationEngine.Adjustment.NONE);
        }
    }

    private record LeafResult(List<Choice> path, Integer months) {
    }

    public CombinationResult findOptimalCombination(Long userId, BigDecimal goalAmount) {
        UserFinancialSnapshotService.Snapshot snapshot = snapshotService.getSnapshot(userId);
        BigDecimal liquidAssetBudget = snapshot.liquidAsset() != null ? snapshot.liquidAsset() : BigDecimal.ZERO;

        List<List<Choice>> groups = new ArrayList<>();
        for (LeverIntensityCalculator.LeverType leverType : LeverIntensityCalculator.LeverType.values()) {
            groups.add(buildChoices(userId, leverType));
        }

        Integer baselineMonths = engine.calculateMonthsToGoalCompound(userId, goalAmount, FutureSimulationEngine.Adjustment.NONE);

        Map<String, LeafResult> memo = new HashMap<>();
        LeafResult best = search(groups, 0, FutureSimulationEngine.Adjustment.NONE, liquidAssetBudget,
                new ArrayList<>(), userId, goalAmount, memo);

        List<LeverChoice> chosen = best.path().stream()
                .filter(choice -> !choice.skipped())
                .map(choice -> new LeverChoice(choice.leverType(), choice.intensity(), false))
                .toList();

        Integer diffMonths = (baselineMonths == null || best.months() == null) ? null : baselineMonths - best.months();
        // 실제로 몇 가지 조합을 비교했는지 — 카드에서 "OO가지 조합을 다 비교해봤어요"처럼 근거로 보여준다.
        int combinationsEvaluated = groups.stream().mapToInt(List::size).reduce(1, (a, b) -> a * b);
        return new CombinationResult(chosen, baselineMonths, best.months(), diffMonths, liquidAssetBudget, combinationsEvaluated);
    }

    // groupIndex번째 그룹부터 나머지를 결정한다. remainingBudget은 지금까지 조기상환에 쓰고 남은 유동자산.
    private LeafResult search(
            List<List<Choice>> groups, int groupIndex,
            FutureSimulationEngine.Adjustment accumulated, BigDecimal remainingBudget,
            List<Choice> pathSoFar, Long userId, BigDecimal goalAmount, Map<String, LeafResult> memo
    ) {
        if (groupIndex == groups.size()) {
            String key = memoKey(accumulated);
            LeafResult cached = memo.get(key);
            if (cached != null) {
                return new LeafResult(List.copyOf(pathSoFar), cached.months());
            }
            Integer months = engine.calculateMonthsToGoalCompound(userId, goalAmount, accumulated);
            LeafResult leaf = new LeafResult(List.copyOf(pathSoFar), months);
            memo.put(key, leaf);
            return leaf;
        }

        LeafResult best = null;
        for (Choice choice : groups.get(groupIndex)) {
            boolean spendsBudget = !choice.skipped() && choice.leverType() == LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT;
            if (spendsBudget && choice.intensity().compareTo(remainingBudget) > 0) {
                continue; // 남은 유동자산보다 큰 조기상환은 애초에 시도하지 않는다(가지치기).
            }

            FutureSimulationEngine.Adjustment nextAccumulated = combine(accumulated, choice.adjustment());
            BigDecimal nextBudget = spendsBudget ? remainingBudget.subtract(choice.intensity()) : remainingBudget;

            pathSoFar.add(choice);
            LeafResult candidate = search(groups, groupIndex + 1, nextAccumulated, nextBudget, pathSoFar, userId, goalAmount, memo);
            pathSoFar.remove(pathSoFar.size() - 1);

            if (isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    // candidate가 currentBest보다 나으면 true. 도달 불가(null)는 항상 최악으로 취급한다.
    private boolean isBetter(LeafResult candidate, LeafResult currentBest) {
        if (currentBest == null) {
            return true;
        }
        if (candidate.months() == null) {
            return false;
        }
        if (currentBest.months() == null) {
            return true;
        }
        return candidate.months() < currentBest.months();
    }

    private FutureSimulationEngine.Adjustment combine(FutureSimulationEngine.Adjustment a, FutureSimulationEngine.Adjustment b) {
        return new FutureSimulationEngine.Adjustment(
                a.liquidAssetsDelta().add(b.liquidAssetsDelta()),
                a.totalDebtDelta().add(b.totalDebtDelta()),
                a.monthlyCashFlowDelta().add(b.monthlyCashFlowDelta())
        );
    }

    private String memoKey(FutureSimulationEngine.Adjustment adjustment) {
        return adjustment.liquidAssetsDelta().setScale(2, RoundingMode.HALF_UP) + "|"
                + adjustment.totalDebtDelta().setScale(2, RoundingMode.HALF_UP) + "|"
                + adjustment.monthlyCashFlowDelta().setScale(2, RoundingMode.HALF_UP);
    }

    private List<Choice> buildChoices(Long userId, LeverIntensityCalculator.LeverType leverType) {
        List<Choice> choices = new ArrayList<>();
        choices.add(Choice.skip(leverType));

        if (!leverCalculator.isLeverAvailable(userId, leverType)) {
            return choices;
        }
        for (BigDecimal intensity : leverCalculator.presetIntensitiesFor(userId, leverType)) {
            FutureSimulationEngine.Adjustment adjustment = leverCalculator.resolveAdjustment(userId, leverType, intensity);
            choices.add(new Choice(leverType, intensity, false, adjustment));
        }
        return choices;
    }
}
