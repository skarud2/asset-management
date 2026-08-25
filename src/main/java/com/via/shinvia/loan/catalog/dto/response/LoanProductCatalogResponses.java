package com.via.shinvia.loan.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class LoanProductCatalogResponses {

    private LoanProductCatalogResponses() {
    }

    public record SyncResult(
            String loanType,
            int pageCount,
            int productCount,
            int optionCount
    ) {
    }

    public record ListEnvelope(
            @JsonProperty("rsp_code") String responseCode,
            @JsonProperty("rsp_message") String responseMessage,
            @JsonProperty("catalog_list") List<Map<String, Object>> catalogList
    ) {
        public static ListEnvelope success(List<Map<String, Object>> catalogs) {
            return new ListEnvelope("00000", "SUCCESS", catalogs);
        }
    }

    public record DetailEnvelope(
            @JsonProperty("rsp_code") String responseCode,
            @JsonProperty("rsp_message") String responseMessage,
            @JsonProperty("catalog") Map<String, Object> catalog
    ) {
        public static DetailEnvelope success(Map<String, Object> catalog) {
            return new DetailEnvelope("00000", "SUCCESS", catalog);
        }
    }
}
