package com.via.shinvia.mydata.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardItemResponseDto {

    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("card_num")
    private String cardNum;

    @JsonProperty("card_name")
    private String cardName;

    @JsonProperty("is_consent")
    private boolean consent;

    @JsonProperty("card_member")
    private String cardMember;

    @JsonProperty("card_type")
    private String cardType;
}