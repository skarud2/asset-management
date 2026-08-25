package com.via.shinvia.account.dto.mock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public final class MockAccountDtos {

    private MockAccountDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountListResponse(
            @JsonProperty("rsp_code")
            String rspCode,

            @JsonProperty("rsp_msg")
            String rspMsg,

            @JsonProperty("search_timestamp")
            String searchTimestamp,

            @JsonProperty("reg_date")
            String regDate,

            @JsonProperty("next_page")
            String nextPage,

            @JsonProperty("account_cnt")
            Integer accountCnt,

            @JsonProperty("account_list")
            List<AccountItem> accountList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountItem(
            @JsonProperty("account_num")
            String accountNum,

            @JsonProperty("account_status")
            String accountStatus,

            @JsonProperty("account_type")
            String accountType,

            @JsonProperty("is_consent")
            Boolean isConsent,

            @JsonProperty("is_foreign_deposit")
            Boolean isForeignDeposit,

            @JsonProperty("is_minus")
            Boolean isMinus,

            @JsonProperty("prod_name")
            String prodName,

            @JsonProperty("seqno")
            String seqno
    ) {
    }

    public record DepositBasicRequest(
            @JsonProperty("org_code")
            String orgCode,

            @JsonProperty("account_num")
            String accountNum,

            @JsonProperty("seqno")
            String seqno,

            @JsonProperty("search_timestamp")
            String searchTimestamp
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositBasicResponse(
            @JsonProperty("rsp_code")
            String rspCode,

            @JsonProperty("rsp_msg")
            String rspMsg,

            @JsonProperty("search_timestamp")
            String searchTimestamp,

            @JsonProperty("basic_cnt")
            Integer basicCnt,

            @JsonProperty("basic_list")
            List<DepositBasicItem> basicList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositBasicItem(
            @JsonProperty("currency_code")
            String currencyCode,

            @JsonProperty("saving_method")
            String savingMethod,

            @JsonProperty("issue_date")
            String issueDate,

            @JsonProperty("exp_date")
            String expDate,

            @JsonProperty("commit_amt")
            BigDecimal commitAmt,

            @JsonProperty("monthly_paid_in_amt")
            BigDecimal monthlyPaidInAmt
    ) {
    }

    public record DepositDetailRequest(
            @JsonProperty("org_code")
            String orgCode,

            @JsonProperty("account_num")
            String accountNum,

            @JsonProperty("seqno")
            String seqno,

            @JsonProperty("search_timestamp")
            String searchTimestamp
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositDetailResponse(
            @JsonProperty("rsp_code")
            String rspCode,

            @JsonProperty("rsp_msg")
            String rspMsg,

            @JsonProperty("search_timestamp")
            String searchTimestamp,

            @JsonProperty("detail_cnt")
            Integer detailCnt,

            @JsonProperty("detail_list")
            List<DepositDetailItem> detailList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositDetailItem(
            @JsonProperty("currency_code")
            String currencyCode,

            @JsonProperty("balance_amt")
            BigDecimal balanceAmt,

            @JsonProperty("withdrawable_amt")
            BigDecimal withdrawableAmt,

            @JsonProperty("offered_rate")
            BigDecimal offeredRate,

            @JsonProperty("last_paid_in_cnt")
            Integer lastPaidInCnt
    ) {
    }

    public record DepositTransactionRequest(
            @JsonProperty("org_code")
            String orgCode,

            @JsonProperty("account_num")
            String accountNum,

            @JsonProperty("seqno")
            String seqno,

            @JsonProperty("from_date")
            String fromDate,

            @JsonProperty("to_date")
            String toDate,

            @JsonProperty("next_page")
            String nextPage,

            @JsonProperty("limit")
            int limit
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositTransactionResponse(
            @JsonProperty("rsp_code")
            String rspCode,

            @JsonProperty("rsp_msg")
            String rspMsg,

            @JsonProperty("next_page")
            String nextPage,

            @JsonProperty("trans_cnt")
            Integer transCnt,

            @JsonProperty("trans_list")
            List<DepositTransactionItem> transList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositTransactionItem(
            @JsonProperty("trans_dtime")
            String transDtime,

            @JsonProperty("trans_no")
            String transNo,

            @JsonProperty("trans_type")
            String transType,

            @JsonProperty("trans_class")
            String transClass,

            @JsonProperty("currency_code")
            String currencyCode,

            @JsonProperty("trans_amt")
            BigDecimal transAmt,

            @JsonProperty("balance_amt")
            BigDecimal balanceAmt,

            @JsonProperty("paid_in_cnt")
            Integer paidInCnt,

            @JsonProperty("trans_memo")
            String transMemo,

            @JsonProperty("category")
            String category
    ) {
    }
}