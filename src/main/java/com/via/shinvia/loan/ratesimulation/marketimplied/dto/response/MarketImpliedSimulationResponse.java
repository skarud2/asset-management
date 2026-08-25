package com.via.shinvia.loan.ratesimulation.marketimplied.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MarketImpliedSimulationResponse(
        Long loanId,
        String loanType,
        String rateType,
        LocalDate asOfDate,
        BigDecimal initialRate,
        List<MarketImpliedRateStep> path,
        boolean truncated,
        // 고정금리 대출 등 계산을 생략한 경우에만 채워짐
        String message,
        String dataSource
) {
}
