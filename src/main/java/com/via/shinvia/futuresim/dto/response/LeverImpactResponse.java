package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;

import java.math.BigDecimal;
import java.util.List;

// GET /api/future-simulation/lever-impact — 4개 레버를 기본 강도로 실행해 추천 카드 목록을 만드는 데 쓴다.
// representativeLoanRatePercent는 조기상환/만기연장 카드가 "이 대출금리가 여유자금 수익률보다 높아서
// 유리해요" 같은 설명 문구를 만들 때 쓰는 값(대출이 없으면 null).
public record LeverImpactResponse(List<LeverItem> levers, BigDecimal representativeLoanRatePercent, LoanSummary baseline) {
    // 기준행과 각 레버의 대출 조건을 한 객체로 내려준다. diff는 기준행 대비값이며, 기존 필드는 그대로 유지한다.
    public record LoanSummary(
            BigDecimal monthlyBurden,
            BigDecimal monthlyBurdenDiff,
            BigDecimal totalInterest,
            BigDecimal totalInterestDiff,
            int repaymentPeriodMonths,
            int repaymentPeriodDiff
    ) {}

    public record LeverItem(
            LeverIntensityCalculator.LeverType leverType,
            boolean available,
            BigDecimal defaultIntensity,
            Integer diffMonths,
            List<PresetItem> presets,
            LoanSummary loanSummary,
            // 클라이언트가 바로 읽을 수 있도록 요청한 상세 필드도 최상위에 병행 제공한다.
            BigDecimal monthlyBurden,
            BigDecimal monthlyBurdenDiff,
            BigDecimal totalInterest,
            BigDecimal totalInterestDiff,
            int repaymentPeriodMonths,
            int repaymentPeriodDiff
    ) {
        // 강도 칩(슬라이더 대신) 하나에 대응하는 강도값과 그 강도에서의 효과 + 상세(월상환액 변화 등).
        public record PresetItem(
                BigDecimal intensity,
                Integer diffMonths,
                LeverIntensityCalculator.LeverDetail detail
        ) {
        }
    }
}
