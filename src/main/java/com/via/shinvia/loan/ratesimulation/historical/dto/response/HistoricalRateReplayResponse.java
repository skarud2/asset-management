package com.via.shinvia.loan.ratesimulation.historical.dto.response;

import com.via.shinvia.loan.ratesimulation.common.dto.response.StagedRateStep;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HistoricalRateReplayResponse(
        Long loanId,
        String loanType,
        String rateType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialRate,
        BigDecimal initialMonthlyPayment,
        List<StagedRateStep> path,
        BigDecimal totalSegmentInterest,
        int changePointCount,
        boolean truncated,
        String disclaimer
) {
}
