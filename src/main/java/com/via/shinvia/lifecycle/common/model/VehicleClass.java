package com.via.shinvia.lifecycle.common.model;

public enum VehicleClass {
    COMPACT("경차"), SMALL("소형"), SEMI_MIDSIZE("준중형"),
    MIDSIZE("중형"), LARGE("준대형"), SUV("SUV");

    private final String label;
    VehicleClass(String label) { this.label = label; }
    public String getLabel() { return label; }
}
