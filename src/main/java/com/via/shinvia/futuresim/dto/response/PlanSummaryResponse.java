package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// GET /api/future-simulation/plans — 저장된 계획 목록 화면(별도 페이지)의 카드 하나.
// 상세 값(레버 선택, 대출 영향 등)은 목록에서 필요 없어서 안 내려준다 — 불러오기는
// FutureSimViewController가 서버에서 바로 세션에 반영하고 5단계로 리다이렉트하는 방식이라
// 별도 상세 조회 API가 없다.
public record PlanSummaryResponse(
        Long id,
        String planName,
        BigDecimal goalAmount,
        Integer projectedMonthsToGoal,
        Integer diffMonths,
        LocalDateTime updatedAt
) {
}
