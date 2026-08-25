package com.via.shinvia.futuresim.dto.request;

import java.math.BigDecimal;
import java.util.List;

// POST /api/future-simulation/plan 요청 — 5단계 "다음" 클릭 시 현재 화면 상태 그대로를 저장한다.
// goalPresetKey는 2단계에서 세션에 남겨두지 않는 값이라 항상 null로 온다(프리셋 없이 직접 입력한
// 경우와 구분이 안 돼 의미 없는 값을 넣느니 null로 두는 게 정직하다).
public record PlanSaveRequest(
        String planName,
        BigDecimal goalAmount,
        String goalPresetKey,
        BigDecimal assumedReturnRate,
        List<ComboSimulationRequest.LeverEntry> levers
) {
}
