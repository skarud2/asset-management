package com.via.shinvia.lifecycle.common.dto;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
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
public class LifecycleEventResult {

    // 계산한 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 계산한 이벤트 종류
    private LifecycleEventType eventType;

    // 실제 시뮬레이션에서 이벤트가 적용된 날짜
    private LocalDate eventDate;

    // 이벤트 적용 직전 금융상태
    private LifecycleFinancialStateDto beforeState;

    // 이벤트 적용 직후 금융상태
    private LifecycleFinancialStateDto afterState;

    // 해당 이벤트에서 발생한 총비용
    private BigDecimal eventCost;

    // 복지 또는 지원으로 절감된 금액
    private BigDecimal supportBenefit;

    // 이벤트 시점에 준비된 자금으로 충당하지 못한 부족금액
    private BigDecimal fundingShortage;

    // 결과 화면에 표시할 간단한 요약
    // 예: "주택구매 시 자기자금 7,500만원이 부족합니다."
    private String summary;
}