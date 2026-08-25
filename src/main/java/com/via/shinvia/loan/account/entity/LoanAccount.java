package com.via.shinvia.loan.account.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccount {

    private Long loanAccountId;

    private Long connectionId;

    private String externalLoanKey;

    private String loanType;

    private BigDecimal principalAmount;

    private BigDecimal currentBalance;

    private BigDecimal interestRate;

    private String rateType;

    private String repaymentType;

    private LocalDate disbursedAt;

    private LocalDate maturityAt;

    private String loanStatus;

    private LocalDateTime dataAsOfAt;

    private LocalDateTime updatedAt;

    private BigDecimal prepaymentFeeRate;

    private LocalDate prepaymentFeeEndDate;
}