package com.via.shinvia.loan.ratesimulation.common.dto.request;

import java.math.BigDecimal;

// StagedRateSimulator.simulateCustomPath 입력 — 임의 시점(monthOffset)에 적용되는 금리
// path의 첫 원소는 monthOffset=0 (시작점)이어야 함
public record CustomRatePathPoint(
        int monthOffset,
        BigDecimal rate
) {
}
