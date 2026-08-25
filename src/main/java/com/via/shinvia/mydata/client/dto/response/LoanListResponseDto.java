package com.via.shinvia.mydata.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LoanListResponseDto {

    @JsonProperty("rsp_code")
    private String rspCode;

    @JsonProperty("rsp_msg")
    private String rspMsg;

    @JsonProperty("search_timestamp")
    private String searchTimestamp;

    @JsonProperty("next_page")
    private String nextPage;

    @JsonProperty("loan_cnt")
    private int loanCnt;

    @JsonProperty("loan_list")
    private List<LoanItemResponseDto> loanList;
}