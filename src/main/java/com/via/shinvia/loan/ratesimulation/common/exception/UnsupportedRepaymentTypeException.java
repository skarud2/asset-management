package com.via.shinvia.loan.ratesimulation.common.exception;

public class UnsupportedRepaymentTypeException extends RuntimeException {

    public UnsupportedRepaymentTypeException(String repaymentType) {
        super("repaymentType=" + repaymentType + "는 지원하지 않는 상환방식이에요");
    }
}
