package com.via.shinvia.loan.catalog.dto.external.mortgage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MortgageResponse(

        @JsonProperty("result")
        Result result
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(

            @JsonProperty("prdt_div")
            String productDivision,

            @JsonProperty("total_count")
            Integer totalCount,

            @JsonProperty("max_page_no")
            Integer maxPageNo,

            @JsonProperty("now_page_no")
            Integer nowPageNo,

            @JsonProperty("err_cd")
            String errorCode,

            @JsonProperty("err_msg")
            String errorMessage,

            @JsonProperty("baseList")
            List<Base> baseList,

            @JsonProperty("optionList")
            List<Option> optionList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Base(

            @JsonProperty("dcls_month")
            String disclosureMonth,

            @JsonProperty("fin_co_no")
            String financeCompanyCode,

            @JsonProperty("fin_prdt_cd")
            String productCode,

            @JsonProperty("kor_co_nm")
            String institutionName,

            @JsonProperty("fin_prdt_nm")
            String productName,

            @JsonProperty("join_way")
            String joinWay,

            @JsonProperty("loan_inci_expn")
            String incidentalExpense,

            @JsonProperty("erly_rpay_fee")
            String earlyRepaymentFee,

            @JsonProperty("dly_rate")
            String delinquencyRate,

            @JsonProperty("loan_lmt")
            String loanLimit,

            @JsonProperty("dcls_strt_day")
            String disclosureStartDay,

            @JsonProperty("dcls_end_day")
            String disclosureEndDay,

            @JsonProperty("fin_co_subm_day")
            String submittedDay
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(

            @JsonProperty("dcls_month")
            String disclosureMonth,

            @JsonProperty("fin_co_no")
            String financeCompanyCode,

            @JsonProperty("fin_prdt_cd")
            String productCode,

            @JsonProperty("mrtg_type")
            String collateralTypeCode,

            @JsonProperty("mrtg_type_nm")
            String collateralTypeName,

            @JsonProperty("rpay_type")
            String repaymentTypeCode,

            @JsonProperty("rpay_type_nm")
            String repaymentTypeName,

            @JsonProperty("lend_rate_type")
            String rateTypeCode,

            @JsonProperty("lend_rate_type_nm")
            String rateTypeName,

            @JsonProperty("lend_rate_min")
            BigDecimal minRate,

            @JsonProperty("lend_rate_max")
            BigDecimal maxRate,

            @JsonProperty("lend_rate_avg")
            BigDecimal averageRate
    ) {
    }
}