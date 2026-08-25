package com.via.shinvia.lifecycle.survey.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class VehicleSurveyRequest {

    @JsonIgnore
    private Long lifecycleEventId;

    // 차량 구매 예정일
    private LocalDate targetDate;

    // 사용자가 계획을 구분하기 위한 차량명
    private String vehicleName;

    private String vehicleModel;

    private String vehicleCondition;

    private Integer annualMileageKm;

    // 차량 구매 가격
    private BigDecimal vehiclePrice;

    // 구매 시 사용할 현금
    private BigDecimal cashPaymentAmount;

    // 차량 구매 시 이용할 대출 예정금액
    private BigDecimal loanAmount;

    // 자동차대출 기간
    private Integer loanPeriodMonths;

    // 구매 후 매월 예상되는 보험료·유류비·정비비 등
    private BigDecimal monthlyMaintenanceCost;
}
