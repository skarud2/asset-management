package com.via.shinvia.policy.recommendation.common.dto;

import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ProductType;
import com.via.shinvia.policy.recommendation.common.model.RecommendationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 상품 추천 판정 결과
public class RecommendationResultDTO {
    private Long productId;
    private String productName;
    private ProductType productType;
    private String institutionName;

    private RecommendationStatus status;
    private Integer matchScore;

    // 상품군이 달라도 같은 카드에서 보여줄 수 있는 공통 표시값
    private String primaryBenefit;
    private String secondaryBenefit;
    private String supportArea;
    private String targetDescription;
    private String eligibilityDescription;
    private String applicationMethod;
    private String relatedUrl;

    private List<ConditionEvaluation> conditions;
}
