package com.via.shinvia.lifecycle.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 추가상환 또는 조기상환 예정일
    private LocalDate targetDate;

    // 상환할 기존 대출 식별자
    // loan_account.loan_account_id
    private Long loanAccountId;

    // 일회성으로 상환할 금액
    private BigDecimal repaymentAmount;

    // 매월 추가로 상환할 금액
    // 일회성 상환만 하면 null 가능
    private BigDecimal additionalMonthlyRepayment;

    // 상환 방식
    // PARTIAL : 부분상환
    // FULL : 전액상환
    private String repaymentAction;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}