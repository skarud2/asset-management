package com.via.shinvia.policy.recommendation.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 자산형성 추천 대상 상품 정보
public class AssetFormationProductDTO {
    private Long productId;
    private String externalId;
    private String productName;
    private String institutionName;
    private String subscriptionTarget;
    private String subscriptionPeriod;
    private String incomeCondition;
    private String ageCondition;
    private String supportRegion;
    private String savingMethod;
    private String governmentSupport;
    private String maturityBenefit;
    private String applicationMethod;
    private String relatedUrl;
}
