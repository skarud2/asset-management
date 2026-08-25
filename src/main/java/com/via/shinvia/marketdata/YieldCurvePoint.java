package com.via.shinvia.marketdata;

import java.math.BigDecimal;

// 수익률곡선의 한 점 (만기 tenorMonths개월짜리 채권의 수익률)
public record YieldCurvePoint(
        int tenorMonths,
        BigDecimal yieldRate
) {
}
