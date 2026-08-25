package com.via.shinvia.mydata.client.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 대출계좌 dto
@Getter
@Setter
@NoArgsConstructor
public class LoanItemResponseDto {

    @JsonProperty("external_loan_key")
    private String externalLoanKey;

    @JsonProperty("loan_type")
    private String loanType;

    @JsonProperty("principal_amount")
    private BigDecimal principalAmount;

    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    @JsonProperty("interest_rate")
    private BigDecimal interestRate;

    @JsonProperty("rate_type")
    private String rateType;

    @JsonProperty("repayment_type")
    private String repaymentType;

    @JsonProperty("disbursed_at")
    private LocalDate disbursedAt;

    @JsonProperty("maturity_at")
    private LocalDate maturityAt;

    @JsonProperty("loan_status")
    private String loanStatus;

    @JsonProperty("data_as_of_at")
    private LocalDateTime dataAsOfAt;

    @JsonProperty("prepayment_fee_rate")
    private BigDecimal prepaymentFeeRate;

    @JsonProperty("prepayment_fee_end_date")
    private LocalDate prepaymentFeeEndDate;

}
