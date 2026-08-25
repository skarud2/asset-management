package com.via.shinvia.client.card.billdetail.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.via.shinvia.client.card.common.response.MydataCardCommonResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CardBillDetailResponse extends MydataCardCommonResponse {

    @JsonProperty("next_page")
    private String nextPage;

    @JsonProperty("bill_detail_cnt")
    private int billDetailCnt;

    @JsonProperty("bill_detail_list")
    private List<CardBillDetailDto> billDetailList;
}
