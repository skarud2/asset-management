package com.via.shinvia.loan.recommendation.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
public class RecommendedLoanProduct {

    private int rank;
    private String badge;

    private Long catalogProductId;
    private String productName;
    private String loanType;
    private String institutionName;
    private String joinWay;

    private BigDecimal recommendedRate;
    private BigDecimal catalogMinRate;
    private BigDecimal catalogMaxRate;
    private BigDecimal maxLimitAmount;
    private Integer maxPeriodMonths;

    private BigDecimal requestedAmount;
    private Integer requestedTermMonths;
    private BigDecimal estimatedMonthlyPayment;
    private BigDecimal averageMonthlyPayment;
    private BigDecimal estimatedTotalInterest;
    private BigDecimal estimatedTotalCost;
    private String paymentLabel;
    private String calculationMethodLabel;

    private String rateBasis;
    private String targetDescription;
    private String reason;
    private String qualificationNote;

    private int preferenceMatchCount;
    private int requestedPreferenceCount;

    private String rateTypeCode;
    private String rateTypeName;
    private String repaymentTypeCode;
    private String repaymentTypeName;
    private String collateralTypeCode;
    private String collateralTypeName;
    private String loanLimitText;

    private BigDecimal optionMinRate;
    private BigDecimal optionMaxRate;
}
