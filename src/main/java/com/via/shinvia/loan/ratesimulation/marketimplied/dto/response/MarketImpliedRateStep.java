package com.via.shinvia.loan.ratesimulation.marketimplied.dto.response;

import java.math.BigDecimal;

public record MarketImpliedRateStep(
        int monthOffset,
        BigDecimal impliedRate,
        BigDecimal monthlyPayment
) {
}
