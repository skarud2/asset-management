package com.via.shinvia.lifecycle.common.model;

public enum SalaryGrowthScenario {
    CONSERVATIVE("보수적"), BASE("기준"), OPTIMISTIC("낙관적"), CUSTOM("직접 입력");

    private final String label;

    SalaryGrowthScenario(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static SalaryGrowthScenario from(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim().replace(" ", "");
        for (SalaryGrowthScenario scenario : values()) {
            if (scenario.name().equalsIgnoreCase(text) ||
                    scenario.label.replace(" ", "").equalsIgnoreCase(text)) {
                return scenario;
            }
        }
        return BASE;
    }
}
