package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FutureSimulationProjectionResponse(
        Integer monthsToGoal,
        List<TimelinePoint> timeline,
        BigDecimal goalAmount,
        BigDecimal benchmarkNetWorth,
        String benchmarkLabel,
        Integer benchmarkCrossMonth,
        boolean crossoverReached,
        BigDecimal monthlyIncome,
        BigDecimal monthlyLivingExpense,
        BigDecimal monthlyLoanPayment,
        BigDecimal monthlySavingsCapacity,
        BigDecimal assumedReturnRatePercent,
        BigDecimal totalLoanBalance
) {
    public record TimelinePoint(
            int monthOffset,
            BigDecimal netWorth,
            BigDecimal contributionAmount,
            BigDecimal returnAmount
    ) {
    }
}
