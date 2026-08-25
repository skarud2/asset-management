package com.via.shinvia.loananalysis.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 대출 상환금액 계산기
@Component
public class LoanPaymentCalculator {

    private static final int MAX_REPAYMENT_MONTHS = 600;

    // 잔여 상환개월 계산
    public int calculateRemainingMonths(
            LocalDate maturityAt
    ) {
        // 만기일 없음
        if (maturityAt == null) {
            return 1;
        }

        // 현재일부터 만기일까지 개월수
        long months = ChronoUnit.MONTHS.between(
                LocalDate.now(),
                maturityAt
        );

        // 최소 1개월 처리
        return (int) Math.max(months, 1);
    }


    // 월 상환액 계산
    public BigDecimal calculateMonthlyPayment(
            BigDecimal balance,
            BigDecimal annualRate,
            int months,
            String repaymentType
    ) {
        validateRepaymentMonths(months);

        // 잘못된 값 처리
        if (balance == null
                || balance.compareTo(BigDecimal.ZERO) <= 0
        ) {

            return BigDecimal.ZERO;
        }

        BigDecimal rate = defaultZero(annualRate);

        // 만기일시상환 계산
        if ("만기일시".equals(repaymentType)) {
            return calculateBulletMonthlyPayment(
                    balance,
                    rate
            );
        }

        // 원금균등상환 계산
        if ("원금균등".equals(repaymentType)) {
            return calculateEqualPrincipalFirstPayment(
                    balance,
                    rate,
                    months
            );
        }

        // 기본 원리금균등상환 계산
        return calculateEqualInstallmentPayment(
                balance,
                rate,
                months
        );
    }


    // 원리금균등 월 상환액
    private BigDecimal calculateEqualInstallmentPayment(
            BigDecimal balance,
            BigDecimal annualRate,
            int months
    ) {
        // 월 금리
        double monthlyRate =
                annualRate.doubleValue()
                        / 100.0
                        / 12.0;

        // 무이자 처리
        if (monthlyRate == 0) {
            return balance.divide(
                    BigDecimal.valueOf(months),
                    0,
                    RoundingMode.HALF_UP
            );
        }

        // 원리금균등 계산
        double payment =
                balance.doubleValue()
                        * monthlyRate
                        * Math.pow(
                        1 + monthlyRate,
                        months
                )
                        / (
                        Math.pow(
                                1 + monthlyRate,
                                months
                        ) - 1
                );

        return BigDecimal.valueOf(payment)
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                );
    }


    // 원금균등 첫 달 상환액
    private BigDecimal calculateEqualPrincipalFirstPayment(
            BigDecimal balance,
            BigDecimal annualRate,
            int months
    ) {
        // 월 원금
        BigDecimal monthlyPrincipal =
                balance.divide(
                        BigDecimal.valueOf(months),
                        10,
                        RoundingMode.HALF_UP
                );

        // 첫 달 이자
        BigDecimal firstMonthInterest =
                calculateMonthlyInterest(
                        balance,
                        annualRate
                );

        return monthlyPrincipal
                .add(firstMonthInterest)
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                );
    }


    // 만기일시 월 이자
    private BigDecimal calculateBulletMonthlyPayment(
            BigDecimal balance,
            BigDecimal annualRate
    ) {
        return calculateMonthlyInterest(
                balance,
                annualRate
        ).setScale(
                0,
                RoundingMode.HALF_UP
        );
    }


    // 월 이자 계산
    private BigDecimal calculateMonthlyInterest(
            BigDecimal balance,
            BigDecimal annualRate
    ) {
        return balance
                .multiply(annualRate)
                .divide(
                        BigDecimal.valueOf(100),
                        10,
                        RoundingMode.HALF_UP
                )
                .divide(
                        BigDecimal.valueOf(12),
                        10,
                        RoundingMode.HALF_UP
                );
    }


    // 예상 잔여 총이자 계산
    public BigDecimal calculateTotalInterest(
            BigDecimal balance,
            BigDecimal annualRate,
            int months,
            String repaymentType
    ) {
        validateRepaymentMonths(months);

        // 잘못된 값 처리
        if (balance == null
                || balance.compareTo(BigDecimal.ZERO) <= 0
        ) {

            return BigDecimal.ZERO;
        }

        BigDecimal rate = defaultZero(annualRate);

        // 만기일시 총이자
        if ("만기일시".equals(repaymentType)) {

            return calculateMonthlyInterest(
                    balance,
                    rate
            )
                    .multiply(
                            BigDecimal.valueOf(months)
                    )
                    .setScale(
                            0,
                            RoundingMode.HALF_UP
                    );
        }

        // 원금균등 총이자
        if ("원금균등".equals(repaymentType)) {

            return calculateEqualPrincipalTotalInterest(
                    balance,
                    rate,
                    months
            );
        }

        // 원리금균등 총이자
        BigDecimal monthlyPayment =
                calculateEqualInstallmentPayment(
                        balance,
                        rate,
                        months
                );

        return monthlyPayment
                .multiply(
                        BigDecimal.valueOf(months)
                )
                .subtract(balance)
                .max(BigDecimal.ZERO)
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                );
    }


    private void validateRepaymentMonths(int months) {
        if (months <= 0 || months > MAX_REPAYMENT_MONTHS) {
            throw new IllegalArgumentException(
                    "상환기간은 1개월 이상 600개월 이하여야 합니다."
            );
        }
    }


    // 원금균등 총이자 계산
    private BigDecimal calculateEqualPrincipalTotalInterest(
            BigDecimal balance,
            BigDecimal annualRate,
            int months
    ) {
        // 매달 납부할 원금
        BigDecimal monthlyPrincipal =
                balance.divide(
                        BigDecimal.valueOf(months),
                        10,
                        RoundingMode.HALF_UP
                );

        BigDecimal remainingBalance = balance;
        BigDecimal totalInterest = BigDecimal.ZERO;

        // 월별 이자 합산
        for (int month = 0; month < months; month++) {

            BigDecimal monthlyInterest =
                    calculateMonthlyInterest(
                            remainingBalance,
                            annualRate
                    );

            totalInterest =
                    totalInterest.add(
                            monthlyInterest
                    );

            remainingBalance =
                    remainingBalance.subtract(
                            monthlyPrincipal
                    );

            // 음수잔액 방지
            if (remainingBalance.compareTo(
                    BigDecimal.ZERO
            ) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
        }

        return totalInterest.setScale(
                0,
                RoundingMode.HALF_UP
        );
    }


    // 중도상환수수료 계산
    public BigDecimal calculatePrepaymentFee(
            BigDecimal repaymentAmount,
            BigDecimal feeRate,
            LocalDate feeEndDate
    ) {
        // 상환금액 없음
        if (repaymentAmount == null
                || repaymentAmount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return BigDecimal.ZERO;
        }

        // 수수료율 없음
        if (feeRate == null
                || feeRate.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return BigDecimal.ZERO;
        }

        // 수수료 종료
        if (feeEndDate != null
                && feeEndDate.isBefore(
                LocalDate.now()
        )) {

            return BigDecimal.ZERO;
        }

        // 상환금액 × 수수료율
        return repaymentAmount
                .multiply(feeRate)
                .divide(
                        BigDecimal.valueOf(100),
                        0,
                        RoundingMode.HALF_UP
                );
    }


    // null 금액 처리
    private BigDecimal defaultZero(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}
