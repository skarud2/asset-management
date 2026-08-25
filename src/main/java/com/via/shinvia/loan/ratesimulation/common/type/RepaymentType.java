package com.via.shinvia.loan.ratesimulation.common.type;

import com.via.shinvia.loan.ratesimulation.common.exception.UnsupportedRepaymentTypeException;

// loan_account.repayment_type 컬럼 값(원리금균등/원금균등/거치식)을 계산 로직에서 다루기 위한 매핑
public enum RepaymentType {

    EQUAL_PRINCIPAL_INTEREST("원리금균등"),
    EQUAL_PRINCIPAL("원금균등"),
    // 거치기간 컬럼이 없어 거치기간 없이 원리금균등과 동일하게 단순화 (거치기간이 이미 지났다고 가정)
    GRACE_PERIOD("거치식"),
    BULLET_PAYMENT("만기일시상환");

    private final String dbValue;

    RepaymentType(String dbValue) {
        this.dbValue = dbValue;
    }

    public static RepaymentType fromDbValue(String dbValue) {
        for (RepaymentType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new UnsupportedRepaymentTypeException(dbValue);
    }
}
