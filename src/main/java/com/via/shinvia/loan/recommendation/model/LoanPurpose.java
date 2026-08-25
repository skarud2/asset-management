package com.via.shinvia.loan.recommendation.model;

import java.util.Arrays;

public enum LoanPurpose {

    CREDIT_LIVING("CREDIT_LIVING", "생활자금", "CREDIT"),
    CREDIT_REFINANCE("CREDIT_REFINANCE", "대환·채무정리", "CREDIT"),
    MORTGAGE_HOME_PURCHASE("MORTGAGE_HOME_PURCHASE", "주택구입", "MORTGAGE"),
    MORTGAGE_LIVING_STABILITY("MORTGAGE_LIVING_STABILITY", "생활안정자금", "MORTGAGE"),
    JEONSE_DEPOSIT("JEONSE_DEPOSIT", "전세보증금", "JEONSE");

    private final String code;
    private final String label;
    private final String loanType;

    LoanPurpose(String code, String label, String loanType) {
        this.code = code;
        this.label = label;
        this.loanType = loanType;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getLoanType() {
        return loanType;
    }

    public String getDisplayLabel() {
        String typeLabel = switch (loanType) {
            case "CREDIT" -> "신용대출";
            case "MORTGAGE" -> "주택담보대출";
            case "JEONSE" -> "전세자금대출";
            default -> loanType;
        };
        return label + " · " + typeLabel;
    }

    public static LoanPurpose fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 대출 목적입니다."));
    }
}
