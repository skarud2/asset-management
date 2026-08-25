package com.via.shinvia.policy.recommendation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 추천 판정에 사용하는 회원 + 금융프로필 + 추천설문 통합 정보
public class RecommendationUserDTO {

    private Long userId;
    private Integer age;

    // user_financial_profile
    private BigDecimal annualIncome;
    private String incomeType;
    private String employmentStatus;
    private Integer creditScore;
    private BigDecimal liquidAssetAmount;

    // policy_recommendation_profile
    private String residenceSido;
    private String residenceSigungu;
    private Integer residenceMonths;

    private Integer employmentMonths;
    private Boolean hasIncome;
    private String incomeVerifiable;

    private Integer householdSize;
    private String maritalStatus;
    private Boolean hasChildren;
    private Integer childrenCount;

    private Boolean basicLivelihoodRecipient;
    private Boolean nearPoverty;
    private Boolean singleParentHousehold;
    private Boolean disabled;
    private Boolean selfRelianceYouth;
    private Boolean multiculturalHousehold;
    private Boolean northKoreanDefector;
    private Boolean childHeadedHousehold;
    private Boolean earnedIncomeTaxCreditRecipient;
    private Boolean basicPensionRecipient;
    private Boolean disabilityBenefitRecipient;
    private Boolean jeonseFraudVictim;

    private BigDecimal householdAnnualIncome;
    private BigDecimal householdNetAssetAmount;
    private Boolean homelessHousehold;
    private Boolean householdHead;
    private Boolean prospectiveHouseholdHead;
    private Boolean firstTimeHomeBuyer;

    private String debtDefaultStatus;
    private String overdueStatus;
    private String policyFinanceUsage;
    private String financialEducationStatus;

    private String desiredSupportPurpose;
    private BigDecimal desiredAmount;
    private BigDecimal monthlySavingCapacity;
    private String priorityPreference;
}
