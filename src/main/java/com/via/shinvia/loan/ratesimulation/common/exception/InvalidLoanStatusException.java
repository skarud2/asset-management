package com.via.shinvia.loan.ratesimulation.common.exception;

public class InvalidLoanStatusException extends RuntimeException {

    public InvalidLoanStatusException() {
        super("완제되었거나 연체 중인 대출은 시뮬레이션할 수 없어요");
    }
}
