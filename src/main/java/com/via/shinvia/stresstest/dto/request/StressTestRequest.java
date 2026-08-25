package com.via.shinvia.stresstest.dto.request;

import java.math.BigDecimal;

public record StressTestRequest(
        Long userId,
        BigDecimal rateDeltaPercent,
        BigDecimal incomeDropPercent,
        BigDecimal unexpectedExpenseAmount,
        Integer simulationMonths
) {
}
