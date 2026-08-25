package com.via.shinvia.surplusfund.guideversion.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class GuideCalculationSnapshot {

    private Long surplusFundCalculationId;
    private Long userId;
    private BigDecimal totalCurrentBalance;
    private BigDecimal selectedAccountBalance;
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
