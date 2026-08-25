package com.via.shinvia.loan.recommendation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreditCandidateRow {

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
    private String creditProductTypeName;

    private Long creditLoanRateOptionId;
    private String rateCategoryCode;
    private String rateCategoryName;
    private BigDecimal rateOver900;
    private BigDecimal rate801To900;
    private BigDecimal rate701To800;
    private BigDecimal rate601To700;
    private BigDecimal rate501To600;
    private BigDecimal rate401To500;
    private BigDecimal rate301To400;
    private BigDecimal rate300OrBelow;
    private BigDecimal averageRate;
}
