package com.via.shinvia.client.ecos;

public class EcosApiException extends RuntimeException {

    public EcosApiException(String message) {
        super("ECOS API 오류: " + message);
    }
}
