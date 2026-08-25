package com.via.shinvia.stresstest.dto.response;

import java.math.BigDecimal;

public record StressTestTimelinePoint(
        int monthOffset,
        BigDecimal balance
) {
}
