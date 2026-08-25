package com.via.shinvia.mydata.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class DepositDetailItemDto {
    private String currencyCode;
    private BigDecimal balanceAmt;
}
