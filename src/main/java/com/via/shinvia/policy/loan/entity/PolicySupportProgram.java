package com.via.shinvia.policy.loan.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
// 맞춤대출 상품 데이터 관리 기능
public class PolicySupportProgram {

    private Long policySupportProgramId;
     //공공데이터 API seq
    private String externalSeq;
     //상품명
    private String programName;
     //대상 요약
    private String targetDescription;
    //최대 지원금액·대출한도 원 단위로 저장
    private BigDecimal maxSupportAmount;
     // 최저금리
    private BigDecimal minInterestRate;
    // 최고금리
    private BigDecimal maxInterestRate;
    // 금리 유형
    private String interestRateType;
    // 금리 원문
    private String interestRateDescription;
    // 대출기간
    private String supportPeriodDescription;
     // 상환방법
    private String repaymentMethod;
     //용도
    private String usageDescription;
    // 기관구분
    private String institutionCategory;
    // 제공기관명
    private String offeringInstitutionName;
    // 취급기관
    private String handlingInstitution;
    //지원지역
    private String supportArea;
     // 상세 지원조건
    private String eligibilityDescription;
     // 신청방법
    private String applicationMethod;
    // 문의처
    private String contactDescription;

    //관련 사이트

    private String applicationUrl;


    //  상품 운영기간

    private String operationPeriod;


    //  추가 조건 JSON

    private String eligibilityJson;

      //시작일 API에서 별도 제공하지 않으므로 null 가능

    private LocalDate effectiveFrom;
      //종료일
    private LocalDate effectiveTo;

    private Boolean active;

    public static PolicySupportProgram create(
            String externalSeq,
            String programName,
            String targetDescription,
            BigDecimal maxSupportAmount,
            BigDecimal minInterestRate,
            BigDecimal maxInterestRate,
            String interestRateType,
            String interestRateDescription,
            String supportPeriodDescription,
            String repaymentMethod,
            String usageDescription,
            String institutionCategory,
            String offeringInstitutionName,
            String handlingInstitution,
            String supportArea,
            String eligibilityDescription,
            String applicationMethod,
            String contactDescription,
            String applicationUrl,
            String operationPeriod,
            String eligibilityJson
    ) {
        PolicySupportProgram entity =
                new PolicySupportProgram();

        entity.externalSeq = externalSeq;
        entity.programName = programName;
        entity.targetDescription = targetDescription;
        entity.maxSupportAmount = maxSupportAmount;
        entity.minInterestRate = minInterestRate;
        entity.maxInterestRate = maxInterestRate;
        entity.interestRateType = interestRateType;
        entity.interestRateDescription =
                interestRateDescription;
        entity.supportPeriodDescription =
                supportPeriodDescription;
        entity.repaymentMethod = repaymentMethod;
        entity.usageDescription = usageDescription;
        entity.institutionCategory =
                institutionCategory;
        entity.offeringInstitutionName =
                offeringInstitutionName;
        entity.handlingInstitution =
                handlingInstitution;
        entity.supportArea = supportArea;
        entity.eligibilityDescription =
                eligibilityDescription;
        entity.applicationMethod =
                applicationMethod;
        entity.contactDescription =
                contactDescription;
        entity.applicationUrl = applicationUrl;
        entity.operationPeriod = operationPeriod;
        entity.eligibilityJson = eligibilityJson;
        entity.active = true;

        return entity;
    }

    public void update(
            String programName,
            String targetDescription,
            BigDecimal maxSupportAmount,
            BigDecimal minInterestRate,
            BigDecimal maxInterestRate,
            String interestRateType,
            String interestRateDescription,
            String supportPeriodDescription,
            String repaymentMethod,
            String usageDescription,
            String institutionCategory,
            String offeringInstitutionName,
            String handlingInstitution,
            String supportArea,
            String eligibilityDescription,
            String applicationMethod,
            String contactDescription,
            String applicationUrl,
            String operationPeriod,
            String eligibilityJson
    ) {
        this.programName = programName;
        this.targetDescription =
                targetDescription;
        this.maxSupportAmount =
                maxSupportAmount;
        this.minInterestRate =
                minInterestRate;
        this.maxInterestRate =
                maxInterestRate;
        this.interestRateType =
                interestRateType;
        this.interestRateDescription =
                interestRateDescription;
        this.supportPeriodDescription =
                supportPeriodDescription;
        this.repaymentMethod =
                repaymentMethod;
        this.usageDescription =
                usageDescription;
        this.institutionCategory =
                institutionCategory;
        this.offeringInstitutionName =
                offeringInstitutionName;
        this.handlingInstitution =
                handlingInstitution;
        this.supportArea =
                supportArea;
        this.eligibilityDescription =
                eligibilityDescription;
        this.applicationMethod =
                applicationMethod;
        this.contactDescription =
                contactDescription;
        this.applicationUrl =
                applicationUrl;
        this.operationPeriod =
                operationPeriod;
        this.eligibilityJson =
                eligibilityJson;
        this.active = true;
    }
}
