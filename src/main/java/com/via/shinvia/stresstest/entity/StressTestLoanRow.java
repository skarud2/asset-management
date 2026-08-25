package com.via.shinvia.stresstest.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// LoanBurdenAggregator 계산에 필요한 loan_account 최소 조회 컬럼만 담는 모델.
// loan/ratesimulation의 LoanAccountSummary와 필드가 겹치지만, 이 기능은 loan/ 기존 파일을
// 건드리지 않기로 해서(LoanRepaymentCalculator만 재사용) 별도로 둔다.
@Getter
@Setter
@NoArgsConstructor
public class StressTestLoanRow {

    private Long loanAccountId;

    private String loanType;

    private BigDecimal currentBalance;

    private BigDecimal interestRate;

    private String rateType;

    private String repaymentType;

    private LocalDate maturityAt;

    private String loanStatus;

    private BigDecimal prepaymentFeeRate;

    private LocalDate prepaymentFeeEndDate;
}
