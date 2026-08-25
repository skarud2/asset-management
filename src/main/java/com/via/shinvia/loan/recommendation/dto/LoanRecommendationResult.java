package com.via.shinvia.loan.recommendation.dto;

import com.via.shinvia.loan.recommendation.model.ExcludedLoanProduct;
import com.via.shinvia.loan.recommendation.model.RecommendedLoanProduct;

import java.math.BigDecimal;
import java.util.List;

public record LoanRecommendationResult(
        String loanPurpose,
        String loanPurposeLabel,
        String loanType,
        BigDecimal requestedAmount,
        int termMonths,
        String calculationMethodLabel,
        int sourceProductCount,
        int eligibleProductCount,
        int excludedProductCount,
        List<RecommendedLoanProduct> recommendations,
        List<ExcludedLoanProduct> exclusions
) {
}
