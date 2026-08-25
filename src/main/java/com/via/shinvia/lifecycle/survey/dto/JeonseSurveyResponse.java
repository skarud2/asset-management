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
public class JeonseSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 전세 입주 희망일
    private LocalDate targetDate;

    // 희망 거주지역 시도
    private String regionSido;

    // 희망 거주지역 시군구
    private String regionSigungu;

    // 희망 주택유형
    // 아파트, 빌라, 오피스텔 등
    private String housingType;

    // 희망 전용면적
    private BigDecimal desiredArea;

    // 주거 수준
    private LifestyleLevel lifestyleLevel;

    // 사용자가 희망하는 전세보증금
    // 직접 입력하지 않으면 null 가능
    private BigDecimal desiredJeonseAmount;

    // 전세 입주 시 사용할 자기자금
    private BigDecimal ownFundAmount;

    // 사용자가 예상하는 전세대출 금액
    // 아직 미정이면 null 가능
    private BigDecimal desiredLoanAmount;

    // 기존 소유 주택 유지 여부
    private Boolean keepExistingHome;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}