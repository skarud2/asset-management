package com.via.shinvia.mydata.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CardListResponseDto {

    @JsonProperty("rsp_code")
    private String rspCode;

    @JsonProperty("rsp_msg")
    private String rspMsg;

    @JsonProperty("search_timestamp")
    private String searchTimestamp;

    @JsonProperty("next_page")
    private String nextPage;

    @JsonProperty("card_cnt")
    private int cardCnt;

    @JsonProperty("card_list")
    private List<CardItemResponseDto> cardList;
}