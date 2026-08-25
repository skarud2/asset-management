package com.via.shinvia.finprofile;

import java.util.Locale;

public enum EmploymentStatus {
    REGULAR("정규직"),
    CONTRACT("계약직"),
    TEMPORARY("임시직"),
    DISPATCH("파견직"),
    DAILY("일용근로자"),
    FREELANCER("프리랜서"),
    BUSINESS("개인사업자"),
    STUDENT("학생"),
    UNEMPLOYED("무직"),
    OTHER("기타");

    private final String label;

    EmploymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EmploymentStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "재직", "EMPLOYED" -> REGULAR;
            case "SELF_EMPLOYED" -> BUSINESS;
            case "JOB_SEEKER" -> UNEMPLOYED;
            default -> {
                try {
                    yield EmploymentStatus.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield OTHER;
                }
            }
        };
    }
}
