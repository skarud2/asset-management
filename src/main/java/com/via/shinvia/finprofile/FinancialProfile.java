package com.via.shinvia.finprofile;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter @Getter @Builder
public class FinancialProfile {
    private Long userFinancialProfileId;
    private Long userId;
    private BigDecimal annualIncome;
    private IncomeType incomeType;
    private EmploymentStatus employmentStatus;
    private Integer creditScore;
    private BigDecimal liquidAssetAmount;   //현금성 자산
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
