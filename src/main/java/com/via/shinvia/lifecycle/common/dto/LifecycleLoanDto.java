package com.via.shinvia.lifecycle.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleLoanDto {
    // 실제 loan_account 테이블의 대출계좌 식별자
    private Long loanAccountId;
    // 대출 유형
    // 예: CREDIT, JEONSE, MORTGAGE
    private String loanType;
    // 현재 남아있는 대출 잔액
    private BigDecimal currentBalance;
    // 현재 적용 금리
    // 예: 0.035 = 3.5%
    private BigDecimal interestRate;
    // 금리 유형
    // FIXED, VARIABLE
    private String rateType;
    // 상환 방식
    // 원리금균등, 원금균등, 만기일시 등
    private String repaymentType;
    // 대출 만기일
    private LocalDate maturityAt;
    // 중도상환수수료율
    private BigDecimal prepaymentFeeRate;
    // 중도상환수수료 적용 종료일
    private LocalDate prepaymentFeeEndDate;
}