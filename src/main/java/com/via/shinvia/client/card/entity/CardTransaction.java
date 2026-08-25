package com.via.shinvia.client.card.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {

    private Long cardTransactionId;
    private Long cardAccountId;
    private String externalTransactionId;
    private LocalDateTime transactionAt;
    private BigDecimal amount;
    private String merchantName;
}
