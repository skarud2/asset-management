package com.via.shinvia.futuresim.event;

// 4단계 [섹션 2] 금리 리스크 참고 카드의 3가지 시나리오 모드.
// loan/ratesimulation/의 계단식(StagedRateSimulator)·시장내재(MarketImpliedRateSimulator) 엔진과
// 이름을 맞췄다. SIMPLE만 futuresim/ 자체 로직(즉시 한 번 상승)이다.
public enum RateChangeMode {
    SIMPLE,
    STAGED,
    MARKET_IMPLIED
}
