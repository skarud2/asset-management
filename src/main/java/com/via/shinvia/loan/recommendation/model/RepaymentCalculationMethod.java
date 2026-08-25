package com.via.shinvia.loan.recommendation.model;

import java.util.Arrays;

public enum RepaymentCalculationMethod {

    EQUAL_PRINCIPAL_INTEREST(
            "EQUAL_PRINCIPAL_INTEREST",
            "원리금균등",
            "월 예상 납입액"
    ),
    EQUAL_PRINCIPAL(
            "EQUAL_PRINCIPAL",
            "원금균등",
            "첫 달 예상 납입액"
    ),
    BULLET(
            "BULLET",
            "만기일시상환",
            "월 예상 이자"
    );

    private final String code;
    private final String label;
    private final String paymentLabel;

    RepaymentCalculationMethod(String code, String label, String paymentLabel) {
        this.code = code;
        this.label = label;
        this.paymentLabel = paymentLabel;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getPaymentLabel() {
        return paymentLabel;
    }

    public static RepaymentCalculationMethod fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 비용 계산 방식입니다."));
    }
}
