package com.via.shinvia.loan.ratesimulation.staged.dto.request;

import java.math.BigDecimal;

public record StagedRateSimulationRequest(
        Long loanId,
        int repricingCycleMonths,
        BigDecimal stepDeltaPercent,
        int stepCount
) {
}
