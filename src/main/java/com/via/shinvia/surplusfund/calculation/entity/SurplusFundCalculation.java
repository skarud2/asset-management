package com.via.shinvia.surplusfund.calculation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurplusFundCalculation {

    private Long surplusFundCalculationId;

    private Long userId;
    private Long connectionId;

    private BigDecimal totalCurrentBalance;
    private BigDecimal selectedAccountBalance;

    private String selectedAccountIds;

    private LocalDate estimatedNextIncomeDate;
    private LocalDate adjustedNextIncomeDate;

    private BigDecimal estimatedLivingExpense;
    private BigDecimal adjustedLivingExpense;

    private BigDecimal estimatedScheduledExpense;
    private BigDecimal adjustedScheduledExpense;

    private BigDecimal recommendedEmergencyFund;
    private BigDecimal adjustedEmergencyFund;

    private BigDecimal calculatedSurplusAmount;
    private BigDecimal finalSurplusAmount;
}