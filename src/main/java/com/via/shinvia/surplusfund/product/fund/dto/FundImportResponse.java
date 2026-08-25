package com.via.shinvia.surplusfund.product.fund.dto;

import java.time.LocalDate;

public record FundImportResponse(
        String sourceType,
        LocalDate disclosureBaseDate,
        int csvRowCount,
        int uniqueProductCount,
        int savedDetailCount,
        String message
)  {
}
