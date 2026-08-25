package com.via.shinvia.lifecycle.common.model;

public enum SupportEffectType {

    // 일회성 현금 지급
    // 예: 실제 계좌로 들어오는 현금성 지원
    CASH_INFLOW,

    // 특정 용도로만 사용할 수 있는 바우처
    // 현금자산 자체를 증가시키면 안 됨
    VOUCHER,

    // 일정 기간 매월 지급되는 현금성 지원
    MONTHLY_CASH_INFLOW,

    // 대출금리를 낮춰주는 지원
    INTEREST_REDUCTION,

    // 사용자가 일정 금액을 저축하면
    // 정부 또는 기관이 추가로 매칭해주는 지원
    MATCHING_CONTRIBUTION,

    // 세금 또는 세액을 줄여주는 지원
    TAX_BENEFIT,

    // 지원금이 아니라 대출 형태로 제공되는 금융지원
    // 자산뿐 아니라 부채에도 반영해야 함
    LOAN
}