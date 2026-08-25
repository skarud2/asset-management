package com.via.shinvia.client.card.bill.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardBillDto {

    @JsonProperty("seqno")
    private String seqno;

    @JsonProperty("charge_amt")
    private BigDecimal chargeAmt;

    @JsonProperty("charge_day")
    private String chargeDay;

    @JsonProperty("charge_month")
    private String chargeMonth;

    @JsonProperty("paid_out_date")
    private LocalDate paidOutDate;
}
