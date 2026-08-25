package com.via.shinvia.loan.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class LoanRecommendationRequest {

    private String loanPurpose = "CREDIT_LIVING";
    private BigDecimal requestedAmount = BigDecimal.valueOf(30_000_000L);
    private Integer termMonths = 36;
    private String calculationMethod = "EQUAL_PRINCIPAL_INTEREST";

    private String preferredRateTypeCode;
    private String preferredRepaymentTypeCode;
    private String preferredCollateralTypeCode;

    public void normalize(String resolvedLoanType) {
        loanPurpose = normalizeUpper(loanPurpose);
        calculationMethod = normalizeUpper(calculationMethod);
        preferredRateTypeCode = normalizeNullable(preferredRateTypeCode);
        preferredRepaymentTypeCode = normalizeNullable(preferredRepaymentTypeCode);
        preferredCollateralTypeCode = normalizeNullable(preferredCollateralTypeCode);

        if (!"MORTGAGE".equals(resolvedLoanType)) {
            preferredCollateralTypeCode = null;
        }
        if ("CREDIT".equals(resolvedLoanType)) {
            preferredRateTypeCode = null;
            preferredRepaymentTypeCode = null;
        }
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
