package com.via.shinvia.loananalysis.dto;

import com.via.shinvia.loananalysis.type.LoanScenarioType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 대출 대안 비교 결과
@Getter
@Builder
public class LoanScenarioResponseDTO {

    // 대안 유형
    private LoanScenarioType scenarioType;

    // 대안 이름
    private String scenarioName;

    // 대출 식별자
    private Long loanAccountId;

    // 변경 전 잔액
    private BigDecimal beforeBalance;

    // 변경 후 잔액
    private BigDecimal afterBalance;

    // 변경 전 금리
    private BigDecimal beforeInterestRate;

    // 변경 후 금리
    private BigDecimal afterInterestRate;

    // 변경 전 월상환액
    private BigDecimal beforeMonthlyPayment;

    // 변경 후 월상환액
    private BigDecimal afterMonthlyPayment;

    // 사용한 상환금액
    private BigDecimal repaymentAmount;

    // 중도상환수수료
    private BigDecimal prepaymentFeeAmount;

    // 대환 부대비용
    private BigDecimal refinanceCostAmount;

    // 대안 실행 후 남는 현금
    private BigDecimal remainingCashAmount;

    // 예상 이자 절감액
    private BigDecimal estimatedInterestSaving;

    // 비용 차감 후 순이익
    private BigDecimal netBenefitAmount;

    // 현금으로 버틸 수 있는 개월
    private BigDecimal liquidityMonths;

    // 1차 추천점수
    private BigDecimal recommendationScore;

    // 추천 설명
    private String recommendationReason;
}