package com.via.shinvia.client.ecos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// ECOS StatisticSearch row 1건 (필요한 필드만 매핑, 나머지는 무시)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcosStatisticRow {

    @JsonProperty("TIME")
    private String time;

    @JsonProperty("DATA_VALUE")
    private String dataValue;
}
