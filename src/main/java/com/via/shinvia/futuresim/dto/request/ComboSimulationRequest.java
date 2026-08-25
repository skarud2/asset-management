package com.via.shinvia.futuresim.dto.request;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;

import java.math.BigDecimal;
import java.util.List;

// POST /api/future-simulation/combo-simulation 요청 바디 — 5단계에서 사용자가 체크한 레버들과
// 각 레버의 강도(4단계와 같은 단위: %/원/개월/원)를 그대로 보낸다.
public record ComboSimulationRequest(
        BigDecimal goalAmount,
        BigDecimal assumedReturnRate,
        List<LeverEntry> levers
) {
    public record LeverEntry(LeverIntensityCalculator.LeverType type, BigDecimal intensity) {
    }
}
