package com.via.shinvia.stresstest.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record StressTestResponse(
        // user_financial_profile에 해당 사용자 행이 없으면(연소득/유동자산 조회 불가) false
        boolean runwayCalculable,
        String runwayUnavailableReason,
        Integer runwayMonths,
        Integer baselineRunwayMonths,
        BigDecimal netMonthlyCashflow,
        BigDecimal availableAssets,
        BigDecimal totalStressedLoanPayment,
        BigDecimal totalCurrentLoanPayment,
        boolean isIncomeDataAvailable,
        BigDecimal stressedMonthlyIncome,
        BigDecimal monthlyLivingExpense,
        boolean isLivingExpenseEstimate,
        String livingExpenseDisclaimer,
        boolean isLiquidAssetDataAvailable,
        BigDecimal totalLiquidAssets,
        List<StressTestTimelinePoint> timeline
) {
}
