package com.via.shinvia.account.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 계좌
@Getter
@Setter
@NoArgsConstructor
public class Account {

    private Long accountId;

    private String accountName;

    private String accountStatus;

    private String accountType;

    private BigDecimal currentBalance;

    private LocalDateTime dataAsOfAt;

    private String externalAccountKey;

    private LocalDate maturityAt;

    private LocalDate openedAt;

    private LocalDateTime updatedAt;

    private Long connectionId;

    private String orgCode;
}