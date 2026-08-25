package com.via.shinvia.report.service.provider;

import java.util.List;

/** 이전 준비중 카드 구현. 실제 금융 라이프 플랜 카드는 LifecycleScenarioCardProvider가 제공한다. */
public class FinancialCyclePlanCardProvider implements ReportCardDataProvider {

    private static final String CARD_KEY = "FINANCIAL_CYCLE_PLAN";

    @Override
    public String getCardKey() {
        return CARD_KEY;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public CardData getCardData(Long userId, Long refId) {
        return new CardData(CARD_KEY, "금융 라이프 플랜", "준비중", "-", List.of(), "아직 연동되지 않은 카드예요.", null, null);
    }
}
