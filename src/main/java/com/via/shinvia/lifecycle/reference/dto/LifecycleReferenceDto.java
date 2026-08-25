package com.via.shinvia.lifecycle.reference.dto;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
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
public class LifecycleReferenceDto {

    // 기준자료 PK
    private Long lifecycleReferenceId;

    // 생애 이벤트
    private LifecycleEventType eventType;

    // 기준값 종류
    private String referenceType;

    // 지역
    private String regionSido;
    private String regionSigungu;

    // 생활 수준
    private LifestyleLevel lifestyleLevel;

    // 금액형 값
    private BigDecimal amountValue;

    // 비율형 값
    private BigDecimal rateValue;

    // 일반 숫자형 값
    private BigDecimal numericValue;

    // 기준시점
    private Integer referenceYear;
    private Integer referenceMonth;

    // 출처
    private String sourceName;
    private String sourceTitle;
    private String sourceUrl;
    private String sourceType;

    // 설명
    private String note;

    // 사용 여부
    private Boolean active;

    // 생성·수정 일시
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}