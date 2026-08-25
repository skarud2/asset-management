package com.via.shinvia.client.card.billdetail.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBillDetailRequest {

    private final String orgCode;
    private final String seqno;
    private final String chargeMonth;
    private final String nextPage;
    private final Integer limit;
}
