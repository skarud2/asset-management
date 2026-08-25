package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;

import java.math.BigDecimal;
import java.util.List;

// GET /api/future-simulation/lever-combination — 레버 4종을 조합해서 찾은 최적 조합(레버 여러 개를 동시에
// 썼을 때 가장 빠른 조합)과, 개별로 했을 때 대비 얼마나 더 당겨지는지를 보여준다.
public record LeverCombinationResponse(
        List<ChosenLever> chosenLevers,
        Integer baselineMonths,
        Integer combinedMonths,
        Integer diffMonths,
        BigDecimal liquidAssetBudget,
        int combinationsEvaluated
) {
    public record ChosenLever(LeverIntensityCalculator.LeverType leverType, BigDecimal intensity) {
    }
}
