package com.via.shinvia.loan.ratesimulation.common.service;

import com.via.shinvia.loan.ratesimulation.common.dto.response.RepaymentCalculationResult;
import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 상환방식별 월 상환액 / 총 이자 계산 (순수 계산 로직, DB 조회 없음)
@Service
public class LoanRepaymentCalculator {

    private static final int MONEY_SCALE = 2;

    public RepaymentCalculationResult calculate(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int remainingMonths,
            String repaymentTypeDbValue
    ) {
        if (remainingMonths <= 0) {
            throw new IllegalArgumentException("remainingMonths는 1 이상이어야 해요");
        }

        RepaymentType repaymentType = RepaymentType.fromDbValue(repaymentTypeDbValue);
        double p = principal.doubleValue();
        double r = annualRatePercent.doubleValue() / 12 / 100;

        return switch (repaymentType) {
            // 거치식: 거치기간 컬럼이 없어 거치기간이 이미 지났다고 가정하고 원리금균등과 동일하게 계산
            case EQUAL_PRINCIPAL_INTEREST, GRACE_PERIOD ->
                    calculateEqualPrincipalInterest(p, r, remainingMonths);
            case EQUAL_PRINCIPAL -> calculateEqualPrincipal(p, r, remainingMonths);
            case BULLET_PAYMENT -> calculateBulletPayment(p, r, remainingMonths);
        };
    }

    private RepaymentCalculationResult calculateBulletPayment(double p, double r, int n) {
        double monthlyInterest = p * r;
        double totalInterest = monthlyInterest * n;
        return toResult(monthlyInterest, totalInterest);
    }

    public int calculateRemainingMonths(LocalDate baseDate, LocalDate maturityAt) {
        return (int) ChronoUnit.MONTHS.between(baseDate, maturityAt);
    }

    public int calculateRemainingMonths(LocalDate maturityAt) {
        return calculateRemainingMonths(LocalDate.now(), maturityAt);
    }

    // 총 상환기간(totalMonths) 중 monthsElapsed개월이 지난 시점의 잔여원금 B(k)
    public BigDecimal calculateRemainingBalance(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int totalMonths,
            int monthsElapsed,
            String repaymentTypeDbValue
    ) {
        if (totalMonths <= 0) {
            throw new IllegalArgumentException("totalMonths는 1 이상이어야 해요");
        }
        if (monthsElapsed < 0 || monthsElapsed > totalMonths) {
            throw new IllegalArgumentException("monthsElapsed는 0 이상 totalMonths 이하이어야 해요");
        }

        RepaymentType repaymentType = RepaymentType.fromDbValue(repaymentTypeDbValue);
        double p = principal.doubleValue();
        double r = annualRatePercent.doubleValue() / 12 / 100;

        double balance = switch (repaymentType) {
            // 거치식: 거치기간 컬럼이 없어 원리금균등과 동일하게 계산 (기존 calculate()와 동일한 단순화)
            case EQUAL_PRINCIPAL_INTEREST, GRACE_PERIOD ->
                    calculateEqualPrincipalInterestBalance(p, r, totalMonths, monthsElapsed);
            case EQUAL_PRINCIPAL -> p - (p / totalMonths) * monthsElapsed;
            case BULLET_PAYMENT -> (monthsElapsed == totalMonths) ? 0.0 : p;
        };

        return toMoney(balance);
    }

    private RepaymentCalculationResult calculateEqualPrincipalInterest(double p, double r, int n) {
        double monthlyPayment = (r == 0)
                ? p / n
                : p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);

        double totalInterest = monthlyPayment * n - p;
        return toResult(monthlyPayment, totalInterest);
    }

    private RepaymentCalculationResult calculateEqualPrincipal(double p, double r, int n) {
        double monthlyPrincipal = p / n;
        double totalInterest = 0;
        double firstMonthPayment = 0;

        for (int k = 1; k <= n; k++) {
            double remainingPrincipal = p - (k - 1) * monthlyPrincipal;
            double monthInterest = remainingPrincipal * r;
            totalInterest += monthInterest;

            if (k == 1) {
                firstMonthPayment = monthlyPrincipal + monthInterest;
            }
        }

        return toResult(firstMonthPayment, totalInterest);
    }

    private double calculateEqualPrincipalInterestBalance(double p, double r, int n, int k) {
        if (r == 0) {
            return p * (n - k) / (double) n;
        }

        double factorN = Math.pow(1 + r, n);
        double factorK = Math.pow(1 + r, k);
        return p * (factorN - factorK) / (factorN - 1);
    }

    private RepaymentCalculationResult toResult(double monthlyPayment, double totalInterest) {
        return new RepaymentCalculationResult(toMoney(monthlyPayment), toMoney(totalInterest));
    }

    private BigDecimal toMoney(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
