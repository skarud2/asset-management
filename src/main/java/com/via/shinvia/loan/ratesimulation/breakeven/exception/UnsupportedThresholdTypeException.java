package com.via.shinvia.loan.ratesimulation.breakeven.exception;

public class UnsupportedThresholdTypeException extends RuntimeException {

    public UnsupportedThresholdTypeException(String thresholdType) {
        super("thresholdType=" + thresholdType + "는 현재 지원하지 않아요. 월소득 데이터 연동 이후 지원 예정이에요 (MONTHLY_PAYMENT_AMOUNT만 지원)");
    }
}
