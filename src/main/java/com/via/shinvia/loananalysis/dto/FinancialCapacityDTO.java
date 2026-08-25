package com.via.shinvia.loananalysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 사용자 재무여력 데이터
@Getter
@Setter
public class FinancialCapacityDTO {

    // 회원 식별자
    private Long userId;

    // 재무프로필 식별자
    private Long userFinancialProfileId;

    // 연소득
    private BigDecimal annualIncome;

    // 소득유형
    private String incomeType;

    // 재직상태
    private String employmentStatus;

    // 신용점수
    private Integer creditScore;

    // 현금성 자산
    private BigDecimal liquidAssetAmount;

    // 최근 월수입
    private BigDecimal totalIncome;

    // 최근 월지출
    private BigDecimal totalExpense;

    // 최근 월저축액
    private BigDecimal savingsAmount;

    // 최근 저축률
    private BigDecimal savingsRate;

    // 지출 변동계수
    private BigDecimal spendingCv;

    // 실제 DSR
    private BigDecimal actualDsr;

    // 스트레스 DSR
    private BigDecimal stressDsr;
}