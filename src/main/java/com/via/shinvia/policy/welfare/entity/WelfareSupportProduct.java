package com.via.shinvia.policy.welfare.entity;

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
// 복합지원상품 DB 모델
public class WelfareSupportProduct {
    private Long welfareSupportProductId;
    private String externalId;
    private String productName;
    private String institutionName;
    private String supportTarget;
    private String ageCondition;
    private String welfareType;
    private String supportContent;
    private String applicationMethod;
    private String responsibleInstitution;
    private String relatedUrl;
    private String sourceType;
    private String regionSido;
    private String regionSigungu;
    private String supportCycle;
    private String supportMethod;
    private String contactInfo;
    private String onlineApplyYn;
    private Boolean active;
    private LocalDateTime sourceUpdatedAt;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
