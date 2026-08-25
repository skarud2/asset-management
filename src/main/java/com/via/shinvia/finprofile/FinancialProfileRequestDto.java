package com.via.shinvia.finprofile;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Builder
public class FinancialProfileRequestDto {
    @NotNull
    private BigDecimal annualIncome;
    @NotNull
    private IncomeType incomeType;
    @NotNull
    private EmploymentStatus employmentStatus;
    @NotNull
    private Integer creditScore;
    @NotNull
    private BigDecimal liquidAssetAmount; //현금성 자산
}
