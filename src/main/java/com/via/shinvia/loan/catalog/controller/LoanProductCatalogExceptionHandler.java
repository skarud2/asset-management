package com.via.shinvia.loan.catalog.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.via.shinvia.loan.catalog.exception.FinlifeApiException;
import com.via.shinvia.loan.catalog.exception.LoanProductCatalogNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.via.shinvia.loan.catalog")
public class LoanProductCatalogExceptionHandler {

    @ExceptionHandler(LoanProductCatalogNotFoundException.class)
    public ResponseEntity<ErrorBody> notFound(LoanProductCatalogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("40400", exception.getMessage()));
    }

    @ExceptionHandler(FinlifeApiException.class)
    public ResponseEntity<ErrorBody> finlife(FinlifeApiException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorBody(exception.getErrorCode(), exception.getMessage()));
    }

    public record ErrorBody(
            @JsonProperty("rsp_code") String code,
            @JsonProperty("rsp_message") String message
    ) {
    }
}
