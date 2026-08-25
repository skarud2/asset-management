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
public class JeonseSurveyRequest {

    // 전세 입주 희망일
    private LocalDate targetDate;

    // 희망 지역
    private String regionSido;

    private String regionSigungu;

    // 아파트, 빌라, 오피스텔 등
    private HousingType housingType;

    // 희망 전용면적
    private BigDecimal desiredArea;

    // 주거 수준
    private LifestyleLevel lifestyleLevel;

    // 사용자가 원하는 전세금
    // 직접 입력하지 않을 경우 기준자료를 사용
    private BigDecimal desiredJeonseAmount;

    // 전세 입주에 사용할 자기자금
    private BigDecimal ownFundAmount;

    // 사용자가 생각하고 있는 전세대출 금액
    // 아직 미정이면 null 가능
    private BigDecimal desiredLoanAmount;

    // 기존 소유 주택이 있을 경우 유지 여부 (true: 유지/보유, false: 매각)
    private Boolean keepExistingHome;
}
