package com.via.shinvia.policy.loan.dto.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 공공데이터 대출상품 응답 전달 기능
public class LoanProductApiItem {

    // 공공데이터 상품 고유번호
    private String seq;

    // 금융상품명
    private String financialProductName;

    // 대출한도
    private String loanLimit;

    // 금리 구분
    private String interestRateCategory;

    // 금리
    private String interestRate;

    // 최대 총 대출기간
    private String maxTotalLoanTerm;

    // 최대 거치기간
    private String maxDeferredTerm;

    // 최대 상환기간
    private String maxRepaymentTerm;

    // 상환방법
    private String repaymentMethod;

    // 용도
    private String usage;

    // 대상
    private String target;

    // 기관 구분
    private String institutionCategory;

    // 제공기관명
    private String offeringInstitutionName;

    // 지원지역
    private String supportArea;

    // 상세 지원조건
    private String supportTargetDetailCondition;

    // 나이조건
    private String age;

    // 소득조건
    private String income;

    // 거주지역 조건
    private String residenceArea;

    // 신용점수 조건
    private String creditScore;

    // 가구조건
    private String householdCondition;

    // 참고 문의처
    private String referenceContact;

    // 보증기관
    private String guaranteeInstitution;

    // 신청방법
    private String joinMethod;

    // 중도상환수수료
    private String repaymentFee;

    // 부대비용
    private String loanIncidentalCost;

    // 연체이자율
    private String overdueInterestRate;

    // 우대금리 조건
    private String preferentialInterestCondition;

    // 기타 참고사항
    private String etcReference;

    // 취급기관
    private String handlingInstitution;

    // 연락처
    private String contact;

    // 관련 사이트
    private String relatedSite;

    // 검색용 대상 필터
    private String targetFilter;

    // 취급기관 상세
    private String handlingInstitutionDetail;

    // 상품분류
    private String productCategory;

    // 운영기간
    private String productOperationPeriod;

    // 금융교육 상품 여부
    private String financialEducationProductYn;

    // 금융교육 상품 기타내용
    private String financialEducationProductEtc;
}
