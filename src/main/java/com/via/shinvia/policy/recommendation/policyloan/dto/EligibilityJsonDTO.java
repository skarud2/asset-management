package com.via.shinvia.policy.recommendation.policyloan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// policy_support_program.eligibility_json 실제 키 구조
public class EligibilityJsonDTO {

    @JsonProperty("age")
    private String age;

    @JsonProperty("income")
    private String income;

    @JsonProperty("residenceArea")
    private String residenceArea;

    @JsonProperty("creditScore")
    private String creditScore;

    @JsonProperty("householdCondition")
    private String householdCondition;

    @JsonProperty("guaranteeInstitution")
    private String guaranteeInstitution;

    @JsonProperty("repaymentFee")
    private String repaymentFee;

    @JsonProperty("loanIncidentalCost")
    private String loanIncidentalCost;

    @JsonProperty("overdueInterestRate")
    private String overdueInterestRate;

    @JsonProperty("preferentialInterestCondition")
    private String preferentialInterestCondition;

    @JsonProperty("etcReference")
    private String etcReference;

    @JsonProperty("handlingInstitutionDetail")
    private String handlingInstitutionDetail;

    @JsonProperty("productCategory")
    private String productCategory;

    @JsonProperty("financialEducationProductYn")
    private String financialEducationProductYn;

    @JsonProperty("financialEducationProductEtc")
    private String financialEducationProductEtc;
}
