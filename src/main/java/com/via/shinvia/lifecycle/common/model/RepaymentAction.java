package com.via.shinvia.lifecycle.common.model;

public enum RepaymentAction {
    PARTIAL("부분 상환"), FULL("전액 상환");

    private final String label;
    RepaymentAction(String label) { this.label = label; }
    public String getLabel() { return label; }
}
