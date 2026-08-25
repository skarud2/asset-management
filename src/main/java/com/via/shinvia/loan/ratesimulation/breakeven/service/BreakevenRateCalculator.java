package com.via.shinvia.loan.ratesimulation.breakeven.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 이분탐색으로 "월 상환액이 threshold를 넘는 최소 금리"를 찾는 순수 계산 로직
@Service
public class BreakevenRateCalculator {

    private static final BigDecimal SEARCH_RANGE_PERCENT = BigDecimal.valueOf(10);
    private static final BigDecimal CONVERGENCE_THRESHOLD_PERCENT = new BigDecimal("0.01");
    private static final int MAX_ITERATIONS = 30;

    private final LoanRepaymentCalculator repaymentCalculator;

    public BreakevenRateCalculator(LoanRepaymentCalculator repaymentCalculator) {
        this.repaymentCalculator = repaymentCalculator;
    }

    public Result search(
            BigDecimal principal,
            BigDecimal currentRatePercent,
            int remainingMonths,
            String repaymentTypeDbValue,
            BigDecimal thresholdMonthlyPayment
    ) {
        BigDecimal currentMonthlyPayment =
                monthlyPaymentAt(principal, currentRatePercent, remainingMonths, repaymentTypeDbValue);

        if (currentMonthlyPayment.compareTo(thresholdMonthlyPayment) >= 0) {
            BigDecimal excessAmount = currentMonthlyPayment.subtract(thresholdMonthlyPayment);
            return new Result(currentMonthlyPayment, null, true, excessAmount);
        }

        BigDecimal lo = currentRatePercent;
        BigDecimal hi = currentRatePercent.add(SEARCH_RANGE_PERCENT);

        BigDecimal hiMonthlyPayment = monthlyPaymentAt(principal, hi, remainingMonths, repaymentTypeDbValue);
        if (hiMonthlyPayment.compareTo(thresholdMonthlyPayment) < 0) {
            // 금리가 10%p 올라도 threshold에 도달하지 않음
            return new Result(currentMonthlyPayment, null, false, null);
        }

        BigDecimal mid = lo;
        int iterations = 0;

        while (iterations < MAX_ITERATIONS && hi.subtract(lo).compareTo(CONVERGENCE_THRESHOLD_PERCENT) > 0) {
            mid = lo.add(hi).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
            BigDecimal midMonthlyPayment = monthlyPaymentAt(principal, mid, remainingMonths, repaymentTypeDbValue);

            if (midMonthlyPayment.compareTo(thresholdMonthlyPayment) < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
            iterations++;
        }

        BigDecimal breakevenRate = mid.setScale(2, RoundingMode.HALF_UP);
        return new Result(currentMonthlyPayment, breakevenRate, false, null);
    }

    private BigDecimal monthlyPaymentAt(
            BigDecimal principal,
            BigDecimal ratePercent,
            int remainingMonths,
            String repaymentTypeDbValue
    ) {
        return repaymentCalculator
                .calculate(principal, ratePercent, remainingMonths, repaymentTypeDbValue)
                .monthlyPayment();
    }

    public record Result(
            BigDecimal currentMonthlyPayment,
            BigDecimal breakevenRate,
            boolean alreadyExceeded,
            // alreadyExceeded=true일 때만 값이 채워짐
            BigDecimal excessAmount
    ) {
        public boolean reached() {
            return breakevenRate != null;
        }
    }
}
