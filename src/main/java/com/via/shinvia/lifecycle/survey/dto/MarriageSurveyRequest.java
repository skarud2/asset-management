package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
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
public class MarriageSurveyRequest {

    // 사용자가 원하는 결혼 예정일
    private LocalDate targetDate;

    // 결혼 시점 예상 거주지역. 지자체 복지 추천에 사용한다.
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

    // 부모님 또는 가족으로부터 지원받을 예정 금액
    private BigDecimal familySupportAmount;

    // CUSTOM 선택 시 사용자가 직접 입력하는 예상 결혼 총비용
    private BigDecimal customEstimatedCost;
}
