package com.via.shinvia.loan.ratesimulation.common.dto.response;

import java.math.BigDecimal;

// 계단식 금리 시나리오의 재산정 시점 1개
public record StagedRateStep(
        int monthOffset,
        BigDecimal appliedRate,
        BigDecimal remainingBalance,
        BigDecimal monthlyPayment,
        // step0(최초 시점)는 구간이 없어 null
        BigDecimal segmentInterest
) {
}
