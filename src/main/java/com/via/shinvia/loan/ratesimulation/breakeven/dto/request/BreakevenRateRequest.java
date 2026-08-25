package com.via.shinvia.loan.ratesimulation.breakeven.dto.request;

import com.via.shinvia.loan.ratesimulation.breakeven.type.ThresholdType;

import java.math.BigDecimal;

public record BreakevenRateRequest(
        Long loanId,
        ThresholdType thresholdType,
        BigDecimal thresholdValue
) {
}
