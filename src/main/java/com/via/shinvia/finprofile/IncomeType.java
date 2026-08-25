package com.via.shinvia.finprofile;

import java.util.Locale;

public enum IncomeType {
    EMPLOYMENT("근로소득"),
    BUSINESS("사업소득"),
    FINANCIAL("금융소득"),
    OTHER("기타소득");

    private final String label;

    IncomeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static IncomeType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "근로", "근로소득" -> EMPLOYMENT;
            case "사업", "사업소득" -> BUSINESS;
            case "금융", "금융소득" -> FINANCIAL;
            case "기타", "기타소득" -> OTHER;
            default -> {
                try {
                    yield IncomeType.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield OTHER;
                }
            }
        };
    }
}
