package com.via.shinvia.surplusfund.guideversion.controller;

import com.via.shinvia.surplusfund.guideversion.exception.GuideVersionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = SurplusFundGuideVersionController.class)
public class SurplusFundGuideVersionExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, messageOf(exception, "요청값을 확인해주세요."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, messageOf(exception, "운용 기록을 찾을 수 없습니다."));
    }

    @ExceptionHandler(GuideVersionConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            GuideVersionConflictException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private String messageOf(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}
