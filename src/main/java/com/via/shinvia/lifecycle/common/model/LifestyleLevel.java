package com.via.shinvia.lifecycle.common.model;

public enum LifestyleLevel {

    // 최소비용 중심의 실속형
    PRACTICAL("실속형"),

    // 조사자료의 평균 수준
    AVERAGE("평균형"),

    // 평균보다 조금 여유로운 수준
    RELAXED("여유형"),

    // 높은 비용을 사용하는 프리미엄 수준
    PREMIUM("프리미엄"),

    // 사용자가 금액을 직접 입력
    CUSTOM("직접 입력");

    private final String label;

    LifestyleLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
