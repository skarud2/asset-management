package com.via.shinvia.surplusfund.product.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// 화면에 보여줄 펀드 info
public record FundProductResponse(
        Long investmentProductId,
        String productCode,
        String productName,
        String providerName,
        String fundType,
        LocalDate disclosureBaseDate,
        BigDecimal return1Month,
        BigDecimal return3Months,
        BigDecimal return6Months,
        BigDecimal return12Months,
        Integer fundGrade,
        BigDecimal upfrontFeeRate,
        BigDecimal totalExpenseRate,
        String sourceType
) {
}
