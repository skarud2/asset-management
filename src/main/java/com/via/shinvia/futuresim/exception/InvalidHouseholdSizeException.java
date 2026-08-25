package com.via.shinvia.futuresim.exception;

public class InvalidHouseholdSizeException extends RuntimeException {

    public InvalidHouseholdSizeException(String householdSize) {
        super("존재하지 않는 가구원수 값이에요: " + householdSize);
    }
}
