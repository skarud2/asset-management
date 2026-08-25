package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;

public record GoalPreviewResponse(
        Integer monthsToGoal,
        BigDecimal currentNetWorth,
        BigDecimal remainingAmount,
        BigDecimal monthlyIncome,
        BigDecimal monthlyLivingExpense,
        BigDecimal monthlyLoanPayment,
        BigDecimal monthlySavingsCapacity,
        boolean savingsCapacityInsufficient,
        String benchmarkComparisonText,
        String benchmarkLabel,
        BigDecimal benchmarkMedianNetWorth
) {
}
