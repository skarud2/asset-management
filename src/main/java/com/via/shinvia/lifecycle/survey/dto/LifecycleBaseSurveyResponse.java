package com.via.shinvia.lifecycle.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleBaseSurveyResponse {

    // 기본 생활정보 식별자
    private Long lifecycleBaseProfileId;

    // 회원 식별자
    private Long userId;

    // 현재 월평균 생활비
    private BigDecimal monthlyLivingExpense;

    // 현재 주거형태
    // FAMILY, MONTHLY_RENT, JEONSE, OWN
    private String currentHousingType;

    // 현재 매월 발생하는 주거비
    // 월세, 관리비 등
    private BigDecimal monthlyHousingExpense;

    // 사용자의 산업군
    // 미래 급여 상승률 기준자료 조회에 사용
    private String industryCode;

    // 미래 급여 상승 시나리오
    // CONSERVATIVE, BASE, OPTIMISTIC, CUSTOM
    private String salaryGrowthScenario;

    // CUSTOM 선택 시 사용자가 직접 입력한 연평균 급여 상승률
    // 예: 0.03 = 3%
    private BigDecimal customSalaryGrowthRate;

    // 최초 저장일시
    private LocalDateTime createdAt;

    // 마지막 수정일시
    private LocalDateTime updatedAt;
}