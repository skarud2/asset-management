package com.via.shinvia.client.card.bill.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBillRequest {

    private final String orgCode;
    private final String fromMonth;
    private final String toMonth;
    private final String nextPage;
    private final Integer limit;
}
