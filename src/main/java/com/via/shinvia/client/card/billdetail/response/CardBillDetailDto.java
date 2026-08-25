package com.via.shinvia.client.card.billdetail.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class CardBillDetailDto {

    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("paid_dtime")
    private String paidDtime;

    @JsonProperty("trans_no")
    private String transNo;

    @JsonProperty("paid_amt")
    private BigDecimal paidAmt;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("merchant_name")
    private String merchantName;

    @JsonProperty("merchant_regno")
    private String merchantRegno;

    @JsonProperty("credit_fee_amt")
    private BigInteger creditFeeAmt;

    @JsonProperty("total_install_cnt")
    private Integer totalInstallCnt;

    @JsonProperty("cur_install_cnt")
    private Integer curInstallCnt;

    @JsonProperty("balance_amt")
    private BigInteger balanceAmt;

    @JsonProperty("prod_type")
    private String prodType;
}
