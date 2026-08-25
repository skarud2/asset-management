package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;
import java.util.List;

// POST /api/future-simulation/combo-simulation 응답 — 3단계 성장곡선과 같은 타임라인 형태(월별 순자산)를
// 기준선(레버 없음)과 조합(선택한 레버 전부 적용) 두 곡선을 겹쳐 그릴 수 있도록 comboMonthsToGoal의
// timeline만 내려준다(기준선 timeline은 /projection에서 이미 받은 걸 재사용).
public record ComboSimulationResponse(
        Integer baselineMonthsToGoal,
        Integer comboMonthsToGoal,
        Integer diffMonths,
        List<TimelinePoint> timeline
) {
    public record TimelinePoint(int monthOffset, BigDecimal netWorth, BigDecimal contributionAmount, BigDecimal returnAmount) {
    }
}
