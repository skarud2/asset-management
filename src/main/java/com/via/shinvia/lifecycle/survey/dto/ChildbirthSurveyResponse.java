package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildbirthSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 출산 예정일
    private LocalDate targetDate;

    // 출생 순서
    // 1 = 첫째, 2 = 둘째, 3 = 셋째
    private Integer childOrder;

    // 둘째 이상 출산 시 기존 일회성 육아용품의 재구매·추가 구매 여부
    private Boolean repurchaseCarSeat;
    private Boolean repurchaseStroller;
    private Boolean repurchaseCrib;
    private Boolean repurchaseOtherSetup;

    // 출산 및 양육 비용 수준
    private LifestyleLevel lifestyleLevel;

    // 산후조리원 이용 여부
    private Boolean postpartumCare;

    // 출산 예정 시점의 거주 시도
    // 예: 서울특별시, 경기도
    private String regionSido;

    // 출산 예정 시점의 거주 시군구
    // 지역별 출산지원금 조회에 사용
    private String regionSigungu;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}
