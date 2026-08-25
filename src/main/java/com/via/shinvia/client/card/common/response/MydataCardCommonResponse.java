package com.via.shinvia.client.card.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * /v2/card/* (MyData 카드 API) 공통 응답 헤더. 레거시 /v2.0 Open Banking 응답과 달리
 * rsp_code, rsp_msg 두 필드만 공통으로 내려온다.
 */
@Data
public class MydataCardCommonResponse {

    @JsonProperty("rsp_code")
    private String rspCode;

    @JsonProperty("rsp_msg")
    private String rspMsg;
}
