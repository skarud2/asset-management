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
public class MarriageSurveyResponse {

    // DB에 저장된 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 해당 이벤트가 속한 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    // 예: 결혼 1 → 출산 2 → 전세 3
    private Integer eventOrder;

    // 결혼 예정일
    private LocalDate targetDate;

    private String regionSido;
    private String regionSigungu;

    // 결혼 비용 수준
    // PRACTICAL, AVERAGE, RELAXED, PREMIUM, CUSTOM
    private LifestyleLevel lifestyleLevel;

    // 예상 하객 수
    private Integer guestCount;

    // 혼수 포함 여부
    private Boolean furnitureIncluded;

    // 신혼여행 포함 여부
    private Boolean honeymoonIncluded;

    // 전체 결혼비용 중 사용자가 부담할 비율
    // 예: 0.5 = 50%
    private BigDecimal userContributionRate;

    // 가족 또는 부모님 지원 예정금액
    private BigDecimal familySupportAmount;

    // CUSTOM 선택 시 사용자가 직접 입력한 결혼 예상비용
    private BigDecimal customEstimatedCost;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}
