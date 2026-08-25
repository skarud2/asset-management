package com.via.shinvia.dsr.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DsrCalculationResultDto {
    // 계산에 적용한 연소득
    private BigDecimal annualIncome;

    // 기존 대출 연간 원리금
    private BigDecimal existingAnnualDebtPayment;

    // 신규대출 연간 원리금
    private BigDecimal newLoanAnnualDebtPayment;

    // 전체 연간 원리금
    private BigDecimal totalAnnualDebtPayment;

    // 일반 예상 DSR
    private BigDecimal dsrRate;

    // 스트레스 금리 적용 후 신규대출 연간 원리금
    private BigDecimal stressNewLoanAnnualDebtPayment;

    // 스트레스 금리 적용 후 전체 연간 원리금
    private BigDecimal stressTotalAnnualDebtPayment;

    // 스트레스 예상 DSR
    private BigDecimal stressDsrRate;

    // 계산에서 제외된 기존 대출
    private List<ExcludedLoanDto> excludedLoans;

    // 일부 대출이 제외되었는지
    private boolean partialCalculation;
}
