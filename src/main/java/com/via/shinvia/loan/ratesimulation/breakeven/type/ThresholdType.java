package com.via.shinvia.loan.ratesimulation.breakeven.type;

// 한계금리 역산 기준값의 종류
// INCOME_RATIO(소득 대비 상환액 비율)는 월소득 데이터가 아직 없어 이번 스코프에서 미지원
public enum ThresholdType {
    MONTHLY_PAYMENT_AMOUNT,
    INCOME_RATIO
}
