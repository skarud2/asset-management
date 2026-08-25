package com.via.shinvia.report.futuresim.dto;

import java.util.List;

public record FuturesimPlanPrintData(
        String planName,
        String savedAt,
        String goalAmount,
        String currentNetWorth,
        String baselineDuration,
        String projectedDuration,
        String diffLabel,
        String finalNetWorth,
        String assumedReturnRate,
        String benchmarkLabel,
        String benchmarkMedianNetWorth,
        List<Lever> levers,
        List<LoanImpact> loanImpacts,
        List<TimelinePoint> baselineTimeline,
        List<TimelinePoint> comboTimeline,
        Integer benchmarkCrossMonth
) {
    public record Lever(String icon, String label, String intensity, String effect, String financialEffect) {
    }

    public record LoanImpact(
            String loanType,
            String beforeMonthlyPayment,
            String afterMonthlyPayment,
            String monthlySaving,
            String repaymentPeriod,
            String totalInterestDiff,
            String prepaymentFee
    ) {
    }

    public record TimelinePoint(
            int monthOffset,
            String netWorth,
            String contributionAmount,
            String returnAmount
    ) {
    }
}
