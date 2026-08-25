package com.via.shinvia.lifecycle.recommendation.adapter;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.loan.recommendation.dto.LoanRecommendationRequest;
import com.via.shinvia.loan.recommendation.dto.LoanRecommendationResult;
import com.via.shinvia.loan.recommendation.model.RecommendedLoanProduct;
import com.via.shinvia.loan.recommendation.service.LoanRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LoanRecommendationAdapter {

    private final LoanRecommendationService loanRecommendationService;

    public List<LifecycleProductDto> recommend(
            String loginEmail,
            LifecycleEventType eventType,
            BigDecimal requestedAmount,
            Integer termMonths,
            int limit
    ) {
        String loanPurpose = toLoanPurpose(eventType);
        if (loginEmail == null || loginEmail.isBlank()
                || loanPurpose == null
                || requestedAmount == null
                || requestedAmount.signum() <= 0
                || limit <= 0) {
            return List.of();
        }

        LoanRecommendationRequest request = new LoanRecommendationRequest();
        request.setLoanPurpose(loanPurpose);
        request.setRequestedAmount(requestedAmount);
        request.setTermMonths(resolveTermMonths(eventType, termMonths));

        try {
            LoanRecommendationResult result =
                    loanRecommendationService.recommend(loginEmail, request);

            return result.recommendations().stream()
                    .limit(limit)
                    .map(this::toLifecycleProduct)
                    .toList();
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private String toLoanPurpose(LifecycleEventType eventType) {
        if (eventType == null) {
            return null;
        }

        return switch (eventType) {
            case MARRIAGE, MONTHLY_RENT -> "CREDIT_LIVING";
            case JEONSE -> "JEONSE_DEPOSIT";
            case HOME_PURCHASE -> "MORTGAGE_HOME_PURCHASE";
            case REPAYMENT -> "CREDIT_REFINANCE";
            case CHILDBIRTH, VEHICLE_PURCHASE -> null;
        };
    }

    private LifecycleProductDto toLifecycleProduct(
            RecommendedLoanProduct product
    ) {
        return LifecycleProductDto.builder()
                .productId(product.getCatalogProductId())
                .productType(toProductType(product.getLoanType()))
                .productName(product.getProductName())
                .institutionName(product.getInstitutionName())
                .recommendationStatus("ELIGIBLE")
                .recommendationScore(toRecommendationScore(product.getRank()))
                .interestRate(toInterestRate(product))
                .loanLimit(firstNotBlank(
                        product.getLoanLimitText(),
                        formatWon(product.getMaxLimitAmount())
                ))
                .loanPeriod(toLoanPeriod(product))
                .repaymentMethod(firstNotBlank(
                        product.getRepaymentTypeName(),
                        product.getCalculationMethodLabel()
                ))
                .relatedUrl(null)
                .build();
    }

    private String toProductType(String loanType) {
        if ("CREDIT".equals(loanType)) {
            return "CREDIT_LOAN";
        }
        if ("MORTGAGE".equals(loanType)) {
            return "MORTGAGE_LOAN";
        }
        if ("JEONSE".equals(loanType)) {
            return "JEONSE_LOAN";
        }
        return "LOAN";
    }

    private Integer resolveTermMonths(
            LifecycleEventType eventType,
            Integer termMonths
    ) {
        if (termMonths != null && termMonths > 0) {
            return termMonths;
        }

        return switch (eventType) {
            case HOME_PURCHASE -> 360;
            case JEONSE -> 24;
            case REPAYMENT -> 60;
            default -> 36;
        };
    }

    private Integer toRecommendationScore(int rank) {
        if (rank <= 0) {
            return null;
        }
        return Math.max(1, 101 - rank);
    }

    private String toInterestRate(RecommendedLoanProduct product) {
        if (product.getRecommendedRate() != null) {
            return product.getRecommendedRate()
                    .stripTrailingZeros()
                    .toPlainString() + "%";
        }
        if (product.getOptionMinRate() != null
                && product.getOptionMaxRate() != null) {
            return product.getOptionMinRate().stripTrailingZeros().toPlainString()
                    + "% ~ "
                    + product.getOptionMaxRate().stripTrailingZeros().toPlainString()
                    + "%";
        }
        if (product.getCatalogMinRate() != null
                && product.getCatalogMaxRate() != null) {
            return product.getCatalogMinRate().stripTrailingZeros().toPlainString()
                    + "% ~ "
                    + product.getCatalogMaxRate().stripTrailingZeros().toPlainString()
                    + "%";
        }
        return null;
    }

    private String toLoanPeriod(RecommendedLoanProduct product) {
        if (product.getMaxPeriodMonths() != null) {
            return "최대 " + product.getMaxPeriodMonths() + "개월";
        }
        if (product.getRequestedTermMonths() != null) {
            return product.getRequestedTermMonths() + "개월";
        }
        return null;
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
    }

    private String firstNotBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
