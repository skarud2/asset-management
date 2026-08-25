package com.via.shinvia.surplusfund.calculation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SurplusFundCalculationSaveRequest {
    @NotEmpty
    private List<Long> selectedAccountIds;

    @NotNull
    private LocalDate adjustedNextIncomeDate;

    @NotNull
    @DecimalMin("0")
    private BigDecimal adjustedLivingExpense;

    @NotNull
    @DecimalMin("0")
    private BigDecimal adjustedScheduledExpense;

    @NotNull
    @DecimalMin("0")
    private BigDecimal adjustedEmergencyFund;

    @NotNull
    @DecimalMin(value = "1")
    private BigDecimal finalSurplusAmount;
}
