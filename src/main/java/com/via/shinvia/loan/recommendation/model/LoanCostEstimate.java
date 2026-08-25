package com.via.shinvia.loan.recommendation.model;

import java.math.BigDecimal;

public record LoanCostEstimate(
        BigDecimal monthlyPayment,
        BigDecimal averageMonthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalCost,
        String paymentLabel,
        String calculationAssumption
) {
}
