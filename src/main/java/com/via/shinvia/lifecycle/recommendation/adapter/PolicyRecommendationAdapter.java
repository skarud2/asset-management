package com.via.shinvia.lifecycle.recommendation.adapter;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationResultDTO;
import com.via.shinvia.policy.recommendation.common.model.ProductType;
import com.via.shinvia.policy.recommendation.common.model.RecommendationStatus;
import com.via.shinvia.policy.recommendation.service.PolicyRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PolicyRecommendationAdapter {

    private final PolicyRecommendationService policyRecommendationService;

    public List<LifecycleProductDto> recommend(
            Long userId,
            LifecycleEventType eventType,
            int limit
    ) {
        if (userId == null || eventType == null || limit <= 0) {
            return List.of();
        }

        try {
            return policyRecommendationService.recommend(userId).stream()
                    .filter(this::isDisplayable)
                    .filter(result -> isRelevant(eventType, result.getProductType()))
                    .filter(result -> isPurposeRelevant(
                            eventType,
                            result.getProductName()
                    ))
                    .limit(limit)
                    .map(this::toLifecycleProduct)
                    .toList();
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private boolean isDisplayable(RecommendationResultDTO result) {
        return result != null
                && result.getStatus() != null
                && result.getStatus() != RecommendationStatus.INELIGIBLE;
    }

    private boolean isRelevant(
            LifecycleEventType eventType,
            ProductType productType
    ) {
        if (productType == null) {
            return false;
        }

        return switch (eventType) {
            case MARRIAGE, CHILDBIRTH, MONTHLY_RENT ->
                    productType == ProductType.ASSET
                            || productType == ProductType.POLICY_LOAN;
            case JEONSE, HOME_PURCHASE ->
                    productType == ProductType.POLICY_LOAN;
            case VEHICLE_PURCHASE, REPAYMENT -> false;
        };
    }

    private boolean isPurposeRelevant(
            LifecycleEventType eventType,
            String productName
    ) {
        if (eventType != LifecycleEventType.MARRIAGE || productName == null) {
            return true;
        }

        String normalizedName = productName.replaceAll("\\s+", "");
        return List.of(
                        "모기지", "주택구입", "주택구매", "구입자금",
                        "전세", "월세", "임차", "보증금", "주거"
                ).stream()
                .noneMatch(normalizedName::contains);
    }

    private LifecycleProductDto toLifecycleProduct(
            RecommendationResultDTO result
    ) {
        ProductType productType = result.getProductType();

        return LifecycleProductDto.builder()
                .productId(result.getProductId())
                .productType(toProductTypeName(productType))
                .productName(result.getProductName())
                .institutionName(result.getInstitutionName())
                .recommendationStatus(result.getStatus().name())
                .recommendationScore(result.getMatchScore())
                .interestRate(toInterestRate(result))
                .loanLimit(result.getPrimaryBenefit())
                .loanPeriod(toLoanPeriod(result))
                .repaymentMethod(null)
                .relatedUrl(result.getRelatedUrl())
                .build();
    }

    private String toProductTypeName(ProductType productType) {
        return productType == ProductType.ASSET
                ? "ASSET_FORMATION"
                : productType.name();
    }

    private String toInterestRate(RecommendationResultDTO result) {
        return result.getProductType() == ProductType.POLICY_LOAN
                ? result.getSecondaryBenefit()
                : null;
    }

    private String toLoanPeriod(RecommendationResultDTO result) {
        return result.getProductType() == ProductType.ASSET
                ? result.getSecondaryBenefit()
                : null;
    }
}
