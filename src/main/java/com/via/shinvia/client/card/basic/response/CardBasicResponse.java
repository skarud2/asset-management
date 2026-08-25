package com.via.shinvia.client.card.basic.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.via.shinvia.client.card.common.response.MydataCardCommonResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

@Data
@EqualsAndHashCode(callSuper = true)
public class CardBasicResponse extends MydataCardCommonResponse {

    @JsonProperty("search_timestamp")
    private String searchTimestamp;

    @JsonProperty("is_trans_payable")
    private Boolean isTransPayable;

    @JsonProperty("is_cash_card")
    private Boolean isCashCard;

    @JsonProperty("linked_bank_code")
    private String linkedBankCode;

    @JsonProperty("account_num")
    private String accountNum;

    @JsonProperty("card_brand")
    private String cardBrand;

    @JsonProperty("annual_fee")
    private BigInteger annualFee;

    @JsonProperty("issue_date")
    private String issueDate;
}
