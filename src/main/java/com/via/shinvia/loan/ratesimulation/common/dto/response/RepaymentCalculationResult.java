package com.via.shinvia.loan.ratesimulation.common.dto.response;

import java.math.BigDecimal;

// LoanRepaymentCalculator 계산 엔진의 출력
// 원금균등상환은 매달 상환액이 달라지므로 monthlyPayment는 1회차(최고액) 기준
public record RepaymentCalculationResult(
        BigDecimal monthlyPayment,
        BigDecimal totalInterest
) {
}
