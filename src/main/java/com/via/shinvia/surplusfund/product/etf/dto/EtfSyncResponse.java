package com.via.shinvia.surplusfund.product.etf.dto;

import java.time.LocalDate;

public record EtfSyncResponse(
        LocalDate baseDate,
        int receivedCount,
        int upsertedCount,
        String sourceType
) {
}
