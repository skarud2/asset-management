package com.via.shinvia.futuresim.event;

import java.math.BigDecimal;

// 금리 변화 시나리오 하나. 모드별로 쓰는 필드가 다르다(STAGED만 3개 다 씀).
// RateChangeEventResolver가 이 이벤트를 실제 금리 경로(List<CustomRatePathPoint>)로 변환한다.
public record RateChangeEvent(
        RateChangeMode mode,
        BigDecimal simpleDeltaPercent,   // SIMPLE 전용 — 1개월 뒤 이만큼 오르고 계속 유지
        Integer repricingCycleMonths,    // STAGED 전용 — 몇 개월마다 재산정되는지
        BigDecimal stepDeltaPercent,     // STAGED 전용 — 재산정마다 오르는 폭
        Integer stepCount                // STAGED 전용 — 총 몇 번 재산정되는지
) {
    private static final BigDecimal DEFAULT_SIMPLE_DELTA_PERCENT = new BigDecimal("1.00");
    private static final int DEFAULT_REPRICING_CYCLE_MONTHS = 6;
    private static final BigDecimal DEFAULT_STEP_DELTA_PERCENT = new BigDecimal("0.25");
    private static final int DEFAULT_STEP_COUNT = 4;

    public static RateChangeEvent simple() {
        return new RateChangeEvent(RateChangeMode.SIMPLE, DEFAULT_SIMPLE_DELTA_PERCENT, null, null, null);
    }

    public static RateChangeEvent staged() {
        return new RateChangeEvent(
                RateChangeMode.STAGED, null, DEFAULT_REPRICING_CYCLE_MONTHS, DEFAULT_STEP_DELTA_PERCENT, DEFAULT_STEP_COUNT
        );
    }

    public static RateChangeEvent marketImplied() {
        return new RateChangeEvent(RateChangeMode.MARKET_IMPLIED, null, null, null, null);
    }

    public static RateChangeEvent of(RateChangeMode mode) {
        return switch (mode) {
            case SIMPLE -> simple();
            case STAGED -> staged();
            case MARKET_IMPLIED -> marketImplied();
        };
    }
}
