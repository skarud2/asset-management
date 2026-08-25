package com.via.shinvia.loan.catalog.exception;

public class LoanProductCatalogNotFoundException extends RuntimeException {
    public LoanProductCatalogNotFoundException(Long catalogProductId) {
        super("Loan product catalog not found: " + catalogProductId);
    }
}
