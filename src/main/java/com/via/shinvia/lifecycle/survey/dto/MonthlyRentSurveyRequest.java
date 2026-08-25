package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.HousingType;
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
public class MonthlyRentSurveyRequest {

    // 월세 입주 희망일
    private LocalDate targetDate;

    // 희망 지역 시도
    private String regionSido;

    // 희망 지역 시군구
    private String regionSigungu;

    // 아파트, 오피스텔, 빌라 등
    private HousingType housingType;

    // 희망 전용면적
    private BigDecimal desiredArea;

    // 주거 수준
    private LifestyleLevel lifestyleLevel;

    // 직접 입력할 경우 희망 보증금
    private BigDecimal desiredDeposit;

    // 직접 입력할 경우 희망 월세
    private BigDecimal desiredMonthlyRent;

    // 예상 월 관리비
    private BigDecimal monthlyManagementFee;

    // 기존 소유 주택이 있을 경우 유지 여부 (true: 유지/보유, false: 매각)
    private Boolean keepExistingHome;
}
