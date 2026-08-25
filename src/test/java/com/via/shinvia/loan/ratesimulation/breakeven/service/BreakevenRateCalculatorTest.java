package com.via.shinvia.loan.ratesimulation.breakeven.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class BreakevenRateCalculatorTest {

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();
    private final BreakevenRateCalculator breakevenRateCalculator = new BreakevenRateCalculator(repaymentCalculator);

    private static final BigDecimal PRINCIPAL = new BigDecimal("100000000");
    private static final BigDecimal CURRENT_RATE = new BigDecimal("4.0");
    private static final int REMAINING_MONTHS = 120;
    private static final String REPAYMENT_TYPE = "원리금균등";

    @Test
    void 이분탐색으로_찾은_한계금리를_다시_계산엔진에_넣으면_threshold와_거의_일치한다() {
        BigDecimal threshold = new BigDecimal("1300000");

        BreakevenRateCalculator.Result result = breakevenRateCalculator.search(
                PRINCIPAL, CURRENT_RATE, REMAINING_MONTHS, REPAYMENT_TYPE, threshold
        );

        assertThat(result.alreadyExceeded()).isFalse();
        assertThat(result.reached()).isTrue();

        BigDecimal monthlyPaymentAtBreakeven = repaymentCalculator.calculate(
                PRINCIPAL, result.breakevenRate(), REMAINING_MONTHS, REPAYMENT_TYPE
        ).monthlyPayment();

        assertThat(monthlyPaymentAtBreakeven.doubleValue())
                .isCloseTo(threshold.doubleValue(), offset(1000.0));
    }

    @Test
    void 이미_현재금리에서_threshold를_초과했으면_alreadyExceeded와_초과액을_반환한다() {
        BigDecimal threshold = new BigDecimal("900000");

        BreakevenRateCalculator.Result result = breakevenRateCalculator.search(
                PRINCIPAL, CURRENT_RATE, REMAINING_MONTHS, REPAYMENT_TYPE, threshold
        );

        assertThat(result.alreadyExceeded()).isTrue();
        assertThat(result.breakevenRate()).isNull();
        assertThat(result.excessAmount().doubleValue())
                .isCloseTo(result.currentMonthlyPayment().subtract(threshold).doubleValue(), offset(0.01));
    }

    @Test
    void 금리가_10퍼센트포인트_올라도_threshold에_도달하지_않으면_breakevenRate는_null이다() {
        BigDecimal unreachableThreshold = new BigDecimal("2000000");

        BreakevenRateCalculator.Result result = breakevenRateCalculator.search(
                PRINCIPAL, CURRENT_RATE, REMAINING_MONTHS, REPAYMENT_TYPE, unreachableThreshold
        );

        assertThat(result.alreadyExceeded()).isFalse();
        assertThat(result.reached()).isFalse();
        assertThat(result.breakevenRate()).isNull();
    }
}
