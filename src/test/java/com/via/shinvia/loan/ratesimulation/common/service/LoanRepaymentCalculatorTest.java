package com.via.shinvia.loan.ratesimulation.common.service;

import com.via.shinvia.loan.ratesimulation.common.dto.response.RepaymentCalculationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class LoanRepaymentCalculatorTest {

    private final LoanRepaymentCalculator calculator = new LoanRepaymentCalculator();

    @Test
    void 원리금균등상환_월상환액과_총이자를_수식대로_계산한다() {
        RepaymentCalculationResult result = calculator.calculate(
                new BigDecimal("100000000"),
                new BigDecimal("4.0"),
                120,
                "원리금균등"
        );

        // p=1억, r=0.04/12, n=120 -> M = p*r*(1+r)^n / ((1+r)^n - 1)
        assertThat(result.monthlyPayment().doubleValue()).isCloseTo(1012451.38, offset(0.01));
        assertThat(result.totalInterest().doubleValue()).isCloseTo(21494165.8, offset(0.01));
    }

    @Test
    void 원금균등상환_1회차_상환액은_원금상환액과_1회차이자의_합이다() {
        RepaymentCalculationResult result = calculator.calculate(
                new BigDecimal("120000000"),
                new BigDecimal("6.0"),
                24,
                "원금균등"
        );

        BigDecimal monthlyPrincipal = new BigDecimal("120000000").divide(BigDecimal.valueOf(24));
        BigDecimal firstMonthInterest = new BigDecimal("120000000")
                .multiply(new BigDecimal("6.0").divide(BigDecimal.valueOf(1200)));
        BigDecimal expectedFirstPayment = monthlyPrincipal.add(firstMonthInterest);

        assertThat(result.monthlyPayment().doubleValue())
                .isCloseTo(expectedFirstPayment.doubleValue(), offset(1.0));
    }

    @Test
    void 거치식은_거치기간_없이_원리금균등과_동일하게_계산한다() {
        RepaymentCalculationResult equalPrincipalInterest = calculator.calculate(
                new BigDecimal("50000000"), new BigDecimal("3.5"), 60, "원리금균등"
        );
        RepaymentCalculationResult gracePeriod = calculator.calculate(
                new BigDecimal("50000000"), new BigDecimal("3.5"), 60, "거치식"
        );

        assertThat(gracePeriod).isEqualTo(equalPrincipalInterest);
    }

    @Test
    void 만기까지_정확히_10년_남은_경우_잔여개월수는_120개월이다() {
        LocalDate today = LocalDate.of(2016, 8, 4);
        LocalDate maturityAt = LocalDate.of(2026, 8, 4);

        int remainingMonths = calculator.calculateRemainingMonths(today, maturityAt);

        assertThat(remainingMonths).isEqualTo(120);
    }

    @Test
    void 원리금균등상환_12개월_경과시점_잔여원금은_월별_상환내역을_직접_시뮬레이션한_값과_일치한다() {
        BigDecimal principal = new BigDecimal("100000000");
        BigDecimal annualRatePercent = new BigDecimal("4.0");
        int totalMonths = 120;
        int monthsElapsed = 12;

        BigDecimal remainingBalance = calculator.calculateRemainingBalance(
                principal, annualRatePercent, totalMonths, monthsElapsed, "원리금균등"
        );

        // 검증용 독립 계산: 월별 상환 스케줄을 직접 시뮬레이션해서 12개월 뒤 잔액을 구함
        // (월상환액 M은 고정, 매달 이자=잔액*r, 원금상환분=M-이자, 잔액-=원금상환분)
        BigDecimal monthlyPayment = calculator.calculate(principal, annualRatePercent, totalMonths, "원리금균등")
                .monthlyPayment();
        double m = monthlyPayment.doubleValue();
        double r = annualRatePercent.doubleValue() / 12 / 100;
        double simulatedBalance = principal.doubleValue();
        for (int month = 1; month <= monthsElapsed; month++) {
            double interest = simulatedBalance * r;
            double principalPortion = m - interest;
            simulatedBalance -= principalPortion;
        }

        assertThat(remainingBalance.doubleValue()).isCloseTo(simulatedBalance, offset(1.0));
        assertThat(remainingBalance.doubleValue()).isCloseTo(91699504.87, offset(1.0));
    }

    @Test
    void 원금균등상환_잔여원금은_원금을_균등분할한_만큼_차감된다() {
        BigDecimal remainingBalance = calculator.calculateRemainingBalance(
                new BigDecimal("120000000"), new BigDecimal("6.0"), 24, 8, "원금균등"
        );

        // B(k) = P - (P/n)*k = 120,000,000 - (120,000,000/24)*8 = 80,000,000
        assertThat(remainingBalance.doubleValue()).isCloseTo(80000000.0, offset(0.01));
    }

    @Test
    void 잔여원금_계산에서_monthsElapsed가_0이면_원금그대로다() {
        BigDecimal remainingBalance = calculator.calculateRemainingBalance(
                new BigDecimal("100000000"), new BigDecimal("4.0"), 120, 0, "원리금균등"
        );

        assertThat(remainingBalance.doubleValue()).isCloseTo(100000000.0, offset(0.01));
    }
}
