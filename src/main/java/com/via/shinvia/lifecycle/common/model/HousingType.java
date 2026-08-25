package com.via.shinvia.lifecycle.common.model;

public enum HousingType {
    APARTMENT("아파트"), VILLA("빌라"), OFFICETEL("오피스텔");

    private final String label;
    HousingType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
