package com.via.shinvia.loan.recommendation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UserFinancialProfileView {

    private Long userFinancialProfileId;
    private Long userId;
    private String loginEmail;
    private String userName;
    private BigDecimal annualIncome;
    private String incomeType;
    private String employmentStatus;
    private Integer creditScore;
    private BigDecimal liquidAssetAmount;
}
