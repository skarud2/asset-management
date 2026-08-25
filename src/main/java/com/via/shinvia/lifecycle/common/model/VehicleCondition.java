package com.via.shinvia.lifecycle.common.model;

public enum VehicleCondition {
    NEW("신차"), USED("중고차");

    private final String label;
    VehicleCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
