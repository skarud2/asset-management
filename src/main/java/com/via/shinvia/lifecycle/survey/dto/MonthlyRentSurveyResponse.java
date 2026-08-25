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
public class MonthlyRentSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 월세 입주 희망일
    private LocalDate targetDate;

    // 희망 거주지역 시도
    private String regionSido;

    // 희망 거주지역 시군구
    private String regionSigungu;

    // 희망 주택유형
    // 아파트, 빌라, 오피스텔 등
    private String housingType;

    // 희망 전용면적
    // 단위는 프로젝트에서 ㎡로 통일하는 것을 권장
    private BigDecimal desiredArea;

    // 주거 수준
    private LifestyleLevel lifestyleLevel;

    // 사용자가 입력한 희망 보증금
    private BigDecimal desiredDeposit;

    // 사용자가 입력한 희망 월세
    private BigDecimal desiredMonthlyRent;

    // 예상 월 관리비
    private BigDecimal monthlyManagementFee;

    // 기존 소유 주택 유지 여부
    private Boolean keepExistingHome;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}