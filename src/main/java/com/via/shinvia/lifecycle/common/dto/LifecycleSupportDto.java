package com.via.shinvia.lifecycle.common.dto;

import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleSupportDto {

    // welfare_support_product 테이블의 식별자
    private Long welfareSupportProductId;

    // 복지지원 또는 서비스명
    private String supportName;

    // 지원 효과가 어떤 방식인지
    // 현금, 바우처, 월지원금 등
    private SupportEffectType effectType;

    // 지원금 또는 지원가치
    private BigDecimal amount;

    // 지원이 지속되는 개월 수
    // 일회성 지원이면 null 또는 0
    private Integer durationMonths;

    // 사용자 조건 기준 추천 상태
    // 예: ELIGIBLE(신청 가능), NEEDS_CONFIRMATION(확인 필요), NOT_ELIGIBLE(대상 아님)
    private String recommendationStatus;

    // 적격성 판정 사유 및 충족 조건 안내
    private String eligibilityReason;

    // 지원정보 출처기관
    // 예: 보건복지부, 복지로, 서민금융진흥원
    private String sourceName;

    // 정보 기준일 / 갱신일시
    private String sourceUpdatedAt;

    // 지원정보 원문 URL
    private String sourceUrl;
}