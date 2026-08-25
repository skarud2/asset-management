package com.via.shinvia.surplusfund.product.etf.client;

public class EtfExternalApiException extends RuntimeException {

    public EtfExternalApiException(String message) {
        super(message);
    }

    public EtfExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

