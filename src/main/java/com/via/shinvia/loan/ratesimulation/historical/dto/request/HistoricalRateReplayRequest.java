package com.via.shinvia.loan.ratesimulation.historical.dto.request;

import java.time.LocalDate;

public record HistoricalRateReplayRequest(
        Long loanId,
        LocalDate startDate,
        LocalDate endDate
) {
}
