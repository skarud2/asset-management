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
public class VehicleSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 차량 구매 예정일
    private LocalDate targetDate;

    // 사용자가 입력한 차량명
    private String vehicleName;

    private String vehicleModel;

    private String vehicleCondition;

    private Integer annualMileageKm;

    // 차량 구매 가격
    private BigDecimal vehiclePrice;

    // 차량 구매에 사용할 현금
    private BigDecimal cashPaymentAmount;

    // 차량 구매를 위해 받을 예정인 대출금액
    private BigDecimal loanAmount;

    // 자동차대출 기간
    // 단위: 개월
    private Integer loanPeriodMonths;

    // 구매 후 월 예상 유지비
    private BigDecimal monthlyMaintenanceCost;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}
