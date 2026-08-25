package com.via.shinvia.client.card.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardBill {
    private Long cardBillId;

    private Long userId;
    private Long connectionId;

    private String seqno;

    private BigDecimal chargeAmount;
    private String chargeDay;
    private String chargeMonth;
    private LocalDate paidOutDate;
}
