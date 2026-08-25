package com.via.shinvia.client.ecos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcosDailyRate(
        LocalDate date,
        BigDecimal rate
) {
}
