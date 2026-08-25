package com.via.shinvia.client.card.bill.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.via.shinvia.client.card.common.response.MydataCardCommonResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CardBillResponse extends MydataCardCommonResponse {

    @JsonProperty("next_page")
    private String nextPage;

    @JsonProperty("bill_cnt")
    private int billCnt;

    @JsonProperty("bill_list")
    private List<CardBillDto> billList;
}
