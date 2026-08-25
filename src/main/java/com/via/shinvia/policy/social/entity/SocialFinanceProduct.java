package com.via.shinvia.policy.social.entity;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 사회연대금융상품 DB 모델
public class SocialFinanceProduct {
    private Long socialFinanceProductId;
    private String externalId;
    private String productName;
    private String institutionName;
    private String productCategory;
    private String supportTarget;
    private String businessType;
    private String supportMethod;
    private String supportAmount;
    private String handlingInstitution;
    private String applicationMethod;
    private String relatedUrl;
    private Boolean active;
    private LocalDateTime sourceUpdatedAt;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
