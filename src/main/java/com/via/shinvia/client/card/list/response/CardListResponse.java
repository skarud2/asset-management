package com.via.shinvia.client.card.list.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.via.shinvia.client.card.common.response.MydataCardCommonResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CardListResponse extends MydataCardCommonResponse {

    @JsonProperty("search_timestamp")
    private String searchTimestamp;

    @JsonProperty("next_page")
    private String nextPage;

    @JsonProperty("card_cnt")
    private int cardCnt;

    @JsonProperty("card_list")
    private List<CardInfoDto> cardList;
}
