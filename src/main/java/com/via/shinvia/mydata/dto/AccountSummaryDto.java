package com.via.shinvia.mydata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDto {
    private String accountNum;
    private String prodName;
    private String accountType;
    private String accountStatus;
    private BigDecimal balanceAmount;
    private String currencyCode;
}
