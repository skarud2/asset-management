package com.via.shinvia.surplusfund.product.etf.controller;

import com.via.shinvia.surplusfund.product.etf.client.EtfExternalApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = {
        EtfProductController.class,
        EtfProductSyncController.class
})
public class EtfProductExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EtfExternalApiException.class)
    public ResponseEntity<ApiError> handleExternalApi(EtfExternalApiException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ApiError(status.value(), message, LocalDateTime.now())
        );
    }

    public record ApiError(
            int status,
            String message,
            LocalDateTime timestamp
    ) {
    }
}
