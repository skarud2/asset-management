package com.via.shinvia.dsr.component;

import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AnnualDebtPaymentCalculator {
    private static final BigDecimal MONTHS_PER_YEAR=BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT_DIVISOR=BigDecimal.valueOf(100);

    private static final int SCALE = 10;

    public BigDecimal calculate(BigDecimal balance, BigDecimal annualInterestRate, int remainingMonths, RepaymentType repaymentType){
        validate(balance, annualInterestRate, remainingMonths);

        RepaymentType appliedType = repaymentType == null ? RepaymentType.EQUAL_PRINCIPAL_INTEREST : repaymentType;

        return switch (appliedType) {
            //원리금균등, 거치식
            case EQUAL_PRINCIPAL_INTEREST, GRACE_PERIOD -> calculateEqualPrincipalInterest( balance, annualInterestRate, remainingMonths);

            //원금균등
            case EQUAL_PRINCIPAL -> calculateEqualPrincipal(balance, annualInterestRate, remainingMonths);

            //만기상환
            case BULLET_PAYMENT -> calculateBulletPayment(balance, annualInterestRate, remainingMonths);
        };

    }

    //원리금 균등 상환
    private BigDecimal calculateEqualPrincipalInterest(BigDecimal balance, BigDecimal annualInterestRate, int remainingMonths) {
        int paymentMonths = Math.min(remainingMonths, 12);
        BigDecimal monthlyRate = toMonthlyRate(annualInterestRate);

        // 무이자일 경우
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal monthlyPayment = balance.divide(BigDecimal.valueOf(remainingMonths), SCALE, RoundingMode.HALF_UP);

            return monthlyPayment
                    .multiply(BigDecimal.valueOf(paymentMonths))
                    .setScale(0, RoundingMode.HALF_UP);
        }

        //월이율이 남은 대출기간 동안 복리로 누적되는 값 (1+월이율)^개월수
        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate).pow(remainingMonths);

        //월 납입액
        BigDecimal monthlyPayment = balance.multiply(monthlyRate).multiply(growthFactor)
                                            .divide(growthFactor.subtract(BigDecimal.ONE), SCALE, RoundingMode.HALF_UP);

        return monthlyPayment.multiply(BigDecimal.valueOf(paymentMonths)).setScale(0, RoundingMode.HALF_UP);
    }

    // 원금균등상환
    private BigDecimal calculateEqualPrincipal(BigDecimal balance, BigDecimal annualInterestRate, int remainingMonths) {
        int paymentMonths = Math.min(remainingMonths, 12);

        BigDecimal monthlyPrincipal = balance.divide(BigDecimal.valueOf(remainingMonths), SCALE, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = toMonthlyRate(annualInterestRate);

        BigDecimal remainingBalance = balance;
        BigDecimal annualPayment = BigDecimal.ZERO;

        for (int month = 0; month < paymentMonths; month++) {
            BigDecimal interest = remainingBalance.multiply(monthlyRate);
            BigDecimal principalPayment = monthlyPrincipal.min(remainingBalance);

            annualPayment = annualPayment.add(principalPayment).add(interest);
            remainingBalance = remainingBalance.subtract(principalPayment);
        }

        return annualPayment.setScale(0, RoundingMode.HALF_UP);
    }

    //만기 상환
    private BigDecimal calculateBulletPayment(BigDecimal balance, BigDecimal annualInterestRate, int remainingMonths) {
        int paymentMonths = Math.min(remainingMonths, 12);
        BigDecimal monthlyRate = toMonthlyRate(annualInterestRate);
        BigDecimal annualInterest = balance.multiply(monthlyRate).multiply(BigDecimal.valueOf(paymentMonths));

        BigDecimal annualPayment = annualInterest;
        if (remainingMonths <= 12) {    //남은 만기가 12개월 이내라면 향후 1년 안에 원금도 상환하므로 원금 전액 포함
            annualPayment = annualPayment.add(balance);
        }

        return annualPayment.setScale(0, RoundingMode.HALF_UP);
    }

    // 전세대출 이자상환액
    public BigDecimal calculateInterestOnly(BigDecimal balance, BigDecimal annualInterestRate) {
        validateAmount(balance);
        validateRate(annualInterestRate);

        BigDecimal decimalRate = annualInterestRate.divide(PERCENT_DIVISOR, SCALE, RoundingMode.HALF_UP);

        return balance.multiply(decimalRate).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal toMonthlyRate(BigDecimal annualInterestRate) {   //연이율-> 월이율
        return annualInterestRate.divide(PERCENT_DIVISOR, SCALE, RoundingMode.HALF_UP)
                .divide(MONTHS_PER_YEAR, SCALE, RoundingMode.HALF_UP);
    }

    private void validate(BigDecimal balance, BigDecimal annualInterestRate, int remainingMonths) {
        validateAmount(balance);
        validateRate(annualInterestRate);

        if (remainingMonths <= 0) {
            throw new IllegalArgumentException(
                    "남은 대출기간은 1개월 이상이어야 합니다."
            );
        }
    }

    private void validateAmount(BigDecimal balance) {   //대출 잔액
        if (balance == null ||
                balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "대출잔액은 0보다 커야 합니다."
            );
        }
    }

    private void validateRate(BigDecimal rate) {    //대출 금리
        if (rate == null ||
                rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "대출금리는 0 이상이어야 합니다."
            );
        }
    }
}
