package com.via.shinvia.loan.ratesimulation.common.exception;

public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long loanAccountId) {
        super("대출을 찾을 수 없어요. loanId=" + loanAccountId);
    }
}
