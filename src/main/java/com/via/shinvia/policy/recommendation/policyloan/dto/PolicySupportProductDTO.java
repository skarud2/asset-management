package com.via.shinvia.policy.recommendation.policyloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 정책대출 추천에 필요한 policy_support_program 조회값
public class PolicySupportProductDTO {
    private Long productId;
    private String externalSeq;
    private String productName;
    private String targetDescription;

    private BigDecimal maxSupportAmount;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private String interestRateDescription;
    private String supportPeriodDescription;
    private String usageDescription;

    private String offeringInstitutionName;
    private String handlingInstitution;
    private String supportArea;
    private String eligibilityDescription;
    private String applicationMethod;
    private String applicationUrl;
    private String operationPeriod;
    private String eligibilityJson;
}
