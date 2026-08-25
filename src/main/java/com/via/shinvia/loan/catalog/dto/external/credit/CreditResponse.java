package com.via.shinvia.loan.catalog.dto.external.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditResponse(
        @JsonProperty("result")
        Result result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("err_cd") String errorCode,
            @JsonProperty("err_msg") String errorMessage,
            @JsonProperty("total_count") Integer totalCount,
            @JsonProperty("max_page_no") Integer maxPageNo,
            @JsonProperty("now_page_no") Integer nowPageNo,
            @JsonProperty("baseList")
            List<Base> baseList,
            @JsonProperty("optionList")
            List<Option> optionList
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Base(
            @JsonProperty("dcls_month") String disclosureMonth,
            @JsonProperty("fin_co_no") String financeCompanyCode,
            @JsonProperty("fin_prdt_cd") String productCode,
            @JsonProperty("crdt_prdt_type") String productTypeCode,
            @JsonProperty("kor_co_nm") String institutionName,
            @JsonProperty("fin_prdt_nm") String productName,
            @JsonProperty("join_way") String joinWay,
            @JsonProperty("cb_name") String creditBureauName,
            @JsonProperty("crdt_prdt_type_nm") String productTypeName,
            @JsonProperty("dcls_strt_day") String disclosureStartDay,
            @JsonProperty("dcls_end_day") String disclosureEndDay,
            @JsonProperty("fin_co_subm_day") String submittedDay
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(
            @JsonProperty("dcls_month") String disclosureMonth,
            @JsonProperty("fin_co_no") String financeCompanyCode,
            @JsonProperty("fin_prdt_cd") String productCode,
            @JsonProperty("crdt_prdt_type") String productTypeCode,
            @JsonProperty("crdt_lend_rate_type") String categoryCode,
            @JsonProperty("crdt_lend_rate_type_nm") String categoryName,
            @JsonProperty("crdt_grad_1") BigDecimal over900,
            @JsonProperty("crdt_grad_4") BigDecimal from801To900,
            @JsonProperty("crdt_grad_5") BigDecimal from701To800,
            @JsonProperty("crdt_grad_6") BigDecimal from601To700,
            @JsonProperty("crdt_grad_10") BigDecimal from501To600,
            @JsonProperty("crdt_grad_11") BigDecimal from401To500,
            @JsonProperty("crdt_grad_12") BigDecimal from301To400,
            @JsonProperty("crdt_grad_13") BigDecimal belowOrEqual300,
            @JsonProperty("crdt_grad_avg") BigDecimal averageRate
    ) {
        public List<BigDecimal> scoreRates() {
            return java.util.stream.Stream.of(
                    over900, from801To900, from701To800, from601To700,
                    from501To600, from401To500, from301To400, belowOrEqual300
            ).filter(Objects::nonNull).toList();
        }
    }
}
