package com.via.shinvia.client.card.basic.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBasicRequest {

    private final String cardId;
    private final String orgCode;
    private final String searchTimestamp;
}
