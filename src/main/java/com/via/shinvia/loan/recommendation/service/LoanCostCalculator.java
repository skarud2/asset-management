package com.via.shinvia.loan.recommendation.service;

import com.via.shinvia.loan.recommendation.model.LoanCostEstimate;
import com.via.shinvia.loan.recommendation.model.RepaymentCalculationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class LoanCostCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    public LoanCostEstimate calculate(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int termMonths,
            RepaymentCalculationMethod method
    ) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("희망 대출금액은 0원보다 커야 합니다.");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("비교 가능한 예상금리가 없습니다.");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("상환기간은 1개월 이상이어야 합니다.");
        }

        return switch (method) {
            case EQUAL_PRINCIPAL_INTEREST -> equalPrincipalInterest(
                    principal,
                    annualRatePercent,
                    termMonths,
                    method
            );
            case EQUAL_PRINCIPAL -> equalPrincipal(
                    principal,
                    annualRatePercent,
                    termMonths,
                    method
            );
            case BULLET -> bullet(
                    principal,
                    annualRatePercent,
                    termMonths,
                    method
            );
        };
    }

    private LoanCostEstimate equalPrincipalInterest(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int termMonths,
            RepaymentCalculationMethod method
    ) {
        double principalValue = principal.doubleValue();
        double monthlyRate = monthlyRate(annualRatePercent).doubleValue();
        BigDecimal monthlyPayment;

        if (monthlyRate == 0.0d) {
            monthlyPayment = principal.divide(
                    BigDecimal.valueOf(termMonths),
                    0,
                    RoundingMode.HALF_UP
            );
        } else {
            double factor = Math.pow(1.0d + monthlyRate, termMonths);
            double payment = principalValue * monthlyRate * factor / (factor - 1.0d);
            monthlyPayment = money(payment);
        }

        BigDecimal totalCost = monthlyPayment
                .multiply(BigDecimal.valueOf(termMonths))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalCost.subtract(principal)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);

        return new LoanCostEstimate(
                monthlyPayment,
                monthlyPayment,
                totalInterest,
                totalCost,
                method.getPaymentLabel(),
                "원리금균등상환 기준의 예상값"
        );
    }

    private LoanCostEstimate equalPrincipal(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int termMonths,
            RepaymentCalculationMethod method
    ) {
        BigDecimal monthlyPrincipal = principal.divide(
                BigDecimal.valueOf(termMonths),
                12,
                RoundingMode.HALF_UP
        );
        BigDecimal monthlyRate = monthlyRate(annualRatePercent);
        BigDecimal firstMonthPayment = monthlyPrincipal
                .add(principal.multiply(monthlyRate))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalInterest = principal
                .multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(termMonths + 1L))
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
        BigDecimal totalCost = principal.add(totalInterest)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal averageMonthlyPayment = totalCost.divide(
                BigDecimal.valueOf(termMonths),
                0,
                RoundingMode.HALF_UP
        );

        return new LoanCostEstimate(
                firstMonthPayment,
                averageMonthlyPayment,
                totalInterest,
                totalCost,
                method.getPaymentLabel(),
                "원금균등상환 기준이며 월 납입액은 매월 감소"
        );
    }

    private LoanCostEstimate bullet(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int termMonths,
            RepaymentCalculationMethod method
    ) {
        BigDecimal monthlyInterest = principal
                .multiply(monthlyRate(annualRatePercent))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalInterest = monthlyInterest
                .multiply(BigDecimal.valueOf(termMonths))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalCost = principal.add(totalInterest)
                .setScale(0, RoundingMode.HALF_UP);

        return new LoanCostEstimate(
                monthlyInterest,
                monthlyInterest,
                totalInterest,
                totalCost,
                method.getPaymentLabel(),
                "만기일시상환 기준이며 만기에 원금을 별도 상환"
        );
    }

    private BigDecimal monthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent
                .divide(ONE_HUNDRED, 16, RoundingMode.HALF_UP)
                .divide(TWELVE, 16, RoundingMode.HALF_UP);
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
    }
}
