package com.via.shinvia.loananalysis.dto;

import com.via.shinvia.dsr.dto.type.LoanType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// 대출 분석용 데이터
@Getter
@Setter
public class LoanAccountAnalysisDTO {

    // 대출 식별자
    private Long loanAccountId;

    // 대출 종류
    private LoanType loanType;

    // 최초 대출원금
    private BigDecimal principalAmount;

    // 현재 대출잔액
    private BigDecimal currentBalance;

    // 현재 적용금리
    private BigDecimal interestRate;

    // 고정·변동금리
    private String rateType;

    // 상환방식
    private String repaymentType;

    // 대출 실행일
    private LocalDate disbursedAt;

    // 대출 만기일
    private LocalDate maturityAt;

    // 정상·완제·연체
    private String loanStatus;

    // 중도상환수수료율
    private BigDecimal prepaymentFeeRate;

    // 중도상환수수료 종료일
    private LocalDate prepaymentFeeEndDate;
}