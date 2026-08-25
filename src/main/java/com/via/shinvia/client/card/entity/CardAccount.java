package com.via.shinvia.client.card.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAccount {

    private Long cardAccountId;
    private Long userId;
    private Long institutionId;
    private Long connectionId;
    private String externalCardKey;
    private String cardName;
    private String cardNumberMasked;
    private Integer paymentDay;
    private LocalDateTime updatedAt;
    private LocalDate issuedAt;
    private LocalDateTime dataAsOfAt;
}
