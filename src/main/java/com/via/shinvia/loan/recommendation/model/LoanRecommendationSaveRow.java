package com.via.shinvia.loan.recommendation.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LoanRecommendationSaveRow {

    private Long catalogProductId;
    private Long userFinancialProfileId;
    private String productName;
    private String loanType;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private BigDecimal maxLimitAmount;
    private Integer maxPeriodMonths;
    private String targetDescription;
    private String institutionName;

    private Integer recommendationRank;
    private BigDecimal recommendedRate;
    private String loanPurpose;
    private BigDecimal requestedAmount;
    private Integer requestedTermMonths;
    private String calculationMethod;
    private BigDecimal estimatedMonthlyPayment;
    private BigDecimal estimatedTotalInterest;
    private BigDecimal estimatedTotalCost;
}
