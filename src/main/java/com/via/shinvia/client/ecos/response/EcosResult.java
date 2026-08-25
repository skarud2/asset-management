package com.via.shinvia.client.ecos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// 데이터가 없거나 요청 파라미터가 잘못된 경우 ECOS가 StatisticSearch 대신 내려주는 에러/안내 블록
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcosResult {

    @JsonProperty("CODE")
    private String code;

    @JsonProperty("MESSAGE")
    private String message;
}
