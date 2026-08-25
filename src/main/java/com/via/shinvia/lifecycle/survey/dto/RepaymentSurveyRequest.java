package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.RepaymentAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSurveyRequest {

    // 추가상환 또는 조기상환 예정일
    private LocalDate targetDate;

    // 상환할 기존 대출 식별자
    // loan_account.loan_account_id
    private Long loanAccountId;

    // 추가상환 예정금액
    private BigDecimal repaymentAmount;

    // 매월 추가로 상환하고 싶은 금액
    // 일회성 상환만 할 경우 null 가능
    private BigDecimal additionalMonthlyRepayment;

    // PARTIAL : 부분상환
    // FULL : 전액상환
    private RepaymentAction repaymentAction;
}
