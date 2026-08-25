package com.via.shinvia.marketdata;

import java.math.BigDecimal;

// 부트스트래핑으로 계산된 구간별 내재 선도금리
// monthOffset은 이 선도금리가 "끝나는" 시점(=해당 tenor)을 의미한다
public record ForwardRatePoint(
        int monthOffset,
        BigDecimal impliedRate
) {
}
