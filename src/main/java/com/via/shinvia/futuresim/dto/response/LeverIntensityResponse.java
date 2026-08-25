package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;

import java.math.BigDecimal;

// GET /api/future-simulation/lever-intensity — 강도 칩 프리셋 3개 외에 사용자가 직접 입력한 값 하나를 계산한다.
public record LeverIntensityResponse(
        LeverIntensityCalculator.LeverType leverType,
        BigDecimal intensity,
        Integer diffMonths,
        LeverIntensityCalculator.LeverDetail detail
) {
}
