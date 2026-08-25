package com.via.shinvia.surplusfund.product.etf.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtfExternalResponse(Response response, Header header, Body body) {

    public Header resolvedHeader() {
        return response == null ? header : response.header();
    }

    public Body resolvedBody() {
        return response == null ? body : response.body();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount,
            Items items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            List<Item> item
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String basDt,
            String srtnCd,
            String isinCd,
            String itmsNm,
            java.math.BigDecimal clpr,
            java.math.BigDecimal vs,
            java.math.BigDecimal fltRt,
            java.math.BigDecimal nav,
            java.math.BigDecimal mkp,
            java.math.BigDecimal hipr,
            java.math.BigDecimal lopr,
            java.math.BigDecimal trqu,
            java.math.BigDecimal trPrc,
            java.math.BigDecimal mrktTotAmt,
            java.math.BigDecimal nPptTotAmt,
            java.math.BigDecimal stLstgCnt,
            String bssIdxIdxNm,
            java.math.BigDecimal bssIdxClpr
    ) {
    }
}
