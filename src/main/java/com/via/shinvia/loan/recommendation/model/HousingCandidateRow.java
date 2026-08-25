package com.via.shinvia.loan.recommendation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class HousingCandidateRow {

    private Long catalogProductId;
    private String productName;
    private String loanType;
    private BigDecimal catalogMinRate;
    private BigDecimal catalogMaxRate;
    private BigDecimal maxLimitAmount;
    private Integer maxPeriodMonths;
    private String targetDescription;
    private String institutionName;
    private String joinWay;

    private Long housingLoanProductOptionId;
    private String collateralTypeCode;
    private String collateralTypeName;
    private String repaymentTypeCode;
    private String repaymentTypeName;
    private String rateTypeCode;
    private String rateTypeName;
    private BigDecimal optionMinRate;
    private BigDecimal optionMaxRate;
    private BigDecimal optionAverageRate;
    private String loanLimitText;
}
