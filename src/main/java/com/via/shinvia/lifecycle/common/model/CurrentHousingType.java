package com.via.shinvia.lifecycle.common.model;

public enum CurrentHousingType {
    FAMILY("가족과 거주"), MONTHLY_RENT("월세"), JEONSE("전세"), OWN("자가");

    private final String label;

    CurrentHousingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static CurrentHousingType from(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim().replace(" ", "");
        for (CurrentHousingType type : values()) {
            if (type.name().equalsIgnoreCase(text) ||
                    type.label.replace(" ", "").equalsIgnoreCase(text)) {
                return type;
            }
        }
        return FAMILY;
    }
}
