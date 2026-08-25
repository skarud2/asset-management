package com.via.shinvia.loananalysis.dto;

import com.via.shinvia.dsr.dto.type.LoanType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 부채 상환순위 결과 DTO
@Getter
@Builder
public class DebtPriorityResponseDTO {

    // 대출 식별자
    private Long loanAccountId;

    // 대출 종류
    private LoanType loanType;

    // 현재 대출잔액
    private BigDecimal currentBalance;

    // 적용 금리
    private BigDecimal interestRate;

    // 금리 유형
    private String rateType;

    // 대출 상태
    private String loanStatus;

    // 연체 원점수
    private BigDecimal overdueScore;

    // 금리 원점수
    private BigDecimal interestScore;

    // 중도상환수수료 원점수
    private BigDecimal feeScore;

    // 소액대출 원점수
    private BigDecimal smallLoanScore;

    // 학자금대출 원점수
    private BigDecimal studentLoanScore;

    // 최종 RPS 점수
    private BigDecimal priorityScore;

    // 상환 우선순위
    private Integer priorityRank;

    // 추천 사유
    private String reason;
}