package com.via.shinvia.futuresim.exception;

public class InvalidAgeException extends RuntimeException {

    public InvalidAgeException(int age) {
        super("유효하지 않은 나이 값이에요: " + age);
    }
}
