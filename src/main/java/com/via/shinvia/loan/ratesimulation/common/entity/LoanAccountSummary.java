package com.via.shinvia.loan.ratesimulation.common.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 한계금리 역산 계산에 필요한 loan_account 최소 조회 컬럼만 담는 모델
// 팀원이 만든 loan 도메인 전체 Entity가 생기면 그쪽으로 대체 가능
@Getter
@Setter
@NoArgsConstructor
public class LoanAccountSummary {

    private Long loanAccountId;

    private String loanType;

    private BigDecimal currentBalance;

    private BigDecimal interestRate;

    private String rateType;

    private String repaymentType;

    private LocalDate maturityAt;

    private LocalDateTime dataAsOfAt;

    private String loanStatus;
}
