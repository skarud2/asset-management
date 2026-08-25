package com.via.shinvia.loan.ratesimulation.historical.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

// 일별 금리 시계열에서 전날과 값이 달라진 지점 (첫 데이터는 시작점으로 무조건 포함, changeBp=0)
public record RateChangePoint(
        LocalDate date,
        BigDecimal newRate,
        int changeBp
) {
}
