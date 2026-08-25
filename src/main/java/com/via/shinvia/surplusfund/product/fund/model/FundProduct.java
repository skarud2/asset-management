package com.via.shinvia.surplusfund.product.fund.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FundProduct {
    // investment_product_catalog
    private Long investmentProductId;
    private String productType;
    private String productCode;
    private String isinCode;
    private String productName;
    private String providerName;
    private String category;
    private String sourceType;
    private boolean active;
    private LocalDateTime lastSyncedAt;

    // fund_product_detail
    private LocalDate disclosureBaseDate;
    private BigDecimal return1Month;
    private BigDecimal return3Months;
    private BigDecimal return6Months;
    private BigDecimal return12Months;
    private Integer fundGrade;
    private BigDecimal upfrontFeeRate;
    private BigDecimal totalExpenseRate;
}
