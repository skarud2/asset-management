package com.via.shinvia.surplusfund.product.fund.dto;

import java.math.BigDecimal;

public record FundCsvRow(
        String providerName,
        String productName,
        BigDecimal return1Month,
        BigDecimal return3Months,
        BigDecimal return6Months,
        BigDecimal return12Months,
        Integer fundGrade,
        String fundType,
        BigDecimal upfrontFeeRate,
        BigDecimal totalExpenseRate
) {
}
