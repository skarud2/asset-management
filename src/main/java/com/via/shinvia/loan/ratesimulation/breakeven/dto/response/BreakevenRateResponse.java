package com.via.shinvia.loan.ratesimulation.breakeven.dto.response;

import com.via.shinvia.loan.ratesimulation.breakeven.type.ThresholdType;

import java.math.BigDecimal;

public record BreakevenRateResponse(
        Long loanId,
        String loanType,
        String rateType,
        BigDecimal currentRate,
        BigDecimal currentMonthlyPayment,
        BigDecimal breakevenRate,
        BigDecimal marginPercent,
        ThresholdType thresholdType,
        BigDecimal thresholdValue,
        boolean alreadyExceeded,
        // alreadyExceeded=true일 때만 채워짐: 현재 월상환액이 기준을 이미 얼마나 초과했는지
        BigDecimal excessAmount,
        String message
) {
}
