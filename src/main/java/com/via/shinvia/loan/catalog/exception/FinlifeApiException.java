package com.via.shinvia.loan.catalog.exception;

public class FinlifeApiException extends RuntimeException {

    private final String errorCode;

    public FinlifeApiException(String errorCode, String message) {
        super("Finlife API error [" + errorCode + "]: " + message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
