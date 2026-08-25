package com.via.shinvia.policy.recommendation.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
// 정책상품 추천 설문 정보
public class PolicyRecommendationProfileDTO {
    private Long policyRecommendationProfileId;
    private Long userId;
    private String residenceSido;
    private String residenceSigungu;
    private Integer residenceMonths;
    private Integer employmentMonths;
    @NotNull
    private Boolean hasIncome;
    private String incomeVerifiable;
    @NotNull
    @Min(1)
    @Max(20)
    private Integer householdSize;

    @NotBlank
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
    @NotNull
    @DecimalMin("0.0")
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
