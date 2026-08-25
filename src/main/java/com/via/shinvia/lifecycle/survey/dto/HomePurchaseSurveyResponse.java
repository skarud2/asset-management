package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
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
public class HomePurchaseSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 주택 구매 희망일
    private LocalDate targetDate;

    // 희망 거주지역 시도
    private String regionSido;

    // 희망 거주지역 시군구
    private String regionSigungu;

    // 희망 주택유형
    // 아파트, 빌라 등
    private String housingType;

    // 희망 전용면적
    private BigDecimal desiredArea;

    // 희망 주택 가격 수준
    private LifestyleLevel lifestyleLevel;

    // 사용자가 직접 입력한 희망 주택가격
    // 기준가격을 사용할 경우 null 가능
    private BigDecimal desiredPurchasePrice;

    // 주택 구매 시 사용할 자기자금
    private BigDecimal ownFundAmount;

    // 주택담보대출 희망 기간
    // 단위: 개월
    private Integer loanPeriodMonths;

    // 희망 상환방식
    // EQUAL_PAYMENT : 원리금균등
    // EQUAL_PRINCIPAL : 원금균등
    // BULLET : 만기일시
    private String repaymentType;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}