package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;

import java.math.BigDecimal;
import java.util.List;

// GET /api/future-simulation/lever-intensity-curve — 카드를 펼칠 때 전체 강도-효과 곡선을 한 번에 받아서,
// 이후 슬라이더 드래그 중에는 클라이언트에서 선형보간만 하고 추가 API 호출은 하지 않는다.
public record LeverIntensityCurveResponse(
        LeverIntensityCalculator.LeverType leverType,
        List<Point> points,
        BigDecimal diminishingReturnIntensity
) {
    public record Point(BigDecimal intensity, Integer diffMonths) {
    }
}
