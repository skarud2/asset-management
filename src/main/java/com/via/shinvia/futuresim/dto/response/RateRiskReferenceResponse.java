package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.event.RateChangeMode;
import com.via.shinvia.loan.ratesimulation.common.dto.response.StagedRateStep;

import java.math.BigDecimal;
import java.util.List;

// 4단계 [섹션 2] 금리 리스크 참고 카드 응답.
// available=false면 참고할 대출이 없다는 뜻(대출 미보유 사용자) — 화면에서 카드 자체를 숨긴다.
public record RateRiskReferenceResponse(
        boolean available,
        RateChangeMode mode,
        String loanType,
        BigDecimal currentBalance,
        BigDecimal currentRate,
        BigDecimal currentMonthlyPayment,
        List<StagedRateStep> path,
        BigDecimal breakevenRate,
        boolean breakevenReached,
        boolean alreadyExceeded,
        BigDecimal thresholdMonthlyPayment
) {
    public static RateRiskReferenceResponse unavailable(RateChangeMode mode) {
        return new RateRiskReferenceResponse(false, mode, null, null, null, null, null, null, false, false, null);
    }
}
