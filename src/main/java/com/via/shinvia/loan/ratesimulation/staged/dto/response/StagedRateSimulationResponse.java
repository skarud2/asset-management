package com.via.shinvia.loan.ratesimulation.staged.dto.response;

import com.via.shinvia.loan.ratesimulation.common.dto.response.StagedRateStep;

import java.math.BigDecimal;
import java.util.List;

public record StagedRateSimulationResponse(
        Long loanId,
        String loanType,
        String rateType,
        Integer repricingCycleMonths,
        BigDecimal stepDeltaPercent,
        BigDecimal initialRate,
        BigDecimal initialMonthlyPayment,
        List<StagedRateStep> path,
        BigDecimal totalSegmentInterest,
        boolean truncated,
        String message
) {
}
