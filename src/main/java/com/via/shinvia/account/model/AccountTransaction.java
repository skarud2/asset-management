package com.via.shinvia.account.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


// 계좌 거래 내역
@Getter
@Setter
@NoArgsConstructor
public class AccountTransaction {

    private Long transactionId;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String categoryCode;

    private String description;

    private String externalTransactionId;

    private String merchantName;

    private LocalDateTime transactionAt;

    private String transactionType;

    private Long accountId;
}