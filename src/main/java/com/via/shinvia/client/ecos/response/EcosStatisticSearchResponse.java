package com.via.shinvia.client.ecos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// 정상 조회 시 StatisticSearch, 에러/데이터없음 시 RESULT 중 하나만 채워져서 옴
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcosStatisticSearchResponse {

    @JsonProperty("StatisticSearch")
    private EcosStatisticSearchBody statisticSearch;

    @JsonProperty("RESULT")
    private EcosResult result;
}
