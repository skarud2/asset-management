package com.via.shinvia.futuresim.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.dto.response.RepaymentCalculationResult;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class LeverLoanComparisonServiceTest {
    private final StressTestLoanMapper loanMapper = mock(StressTestLoanMapper.class);
    private final LoanRepaymentCalculator calculator = new LoanRepaymentCalculator();
    private final LeverLoanComparisonService service = new LeverLoanComparisonService(loanMapper, calculator);

    @Test
    void baseline_sums_all_existing_loan_calculator_results() {
        StressTestLoanRow first = loan("100000000", "4.2", 120);
        StressTestLoanRow second = loan("30000000", "5.1", 48);
        when(loanMapper.findNormalLoansByUserId(7L)).thenReturn(List.of(first, second));

        var actual = service.baseline(7L);
        var firstResult = calculationFor(first, 0, BigDecimal.ZERO);
        var secondResult = calculationFor(second, 0, BigDecimal.ZERO);
        assertThat(actual.monthlyBurden()).isEqualByComparingTo(firstResult.monthlyPayment().add(secondResult.monthlyPayment()));
        assertThat(actual.totalInterest()).isEqualByComparingTo(firstResult.totalInterest().add(secondResult.totalInterest()));
        assertThat(actual.repaymentPeriodMonths()).isEqualTo(120);
    }

    @Test
    void prepayment_recalculates_only_target_loan_with_same_term() {
        StressTestLoanRow target = loan("100000000", "4.2", 120);
        StressTestLoanRow other = loan("30000000", "5.1", 48);
        when(loanMapper.findNormalLoansByUserId(7L)).thenReturn(List.of(target, other));

        var actual = service.forLever(7L, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, new BigDecimal("25000000"));
        var expectedTarget = calculationFor(target, 0, new BigDecimal("25000000"));
        var expectedOther = calculationFor(other, 0, BigDecimal.ZERO);
        assertThat(actual.monthlyBurden()).isEqualByComparingTo(expectedTarget.monthlyPayment().add(expectedOther.monthlyPayment()));
        assertThat(actual.totalInterest()).isEqualByComparingTo(expectedTarget.totalInterest().add(expectedOther.totalInterest()));
        assertThat(actual.repaymentPeriodMonths()).isEqualTo(120);
    }

    @Test
    void term_extension_uses_calculator_with_extended_term_and_exposes_tradeoff() {
        StressTestLoanRow target = loan("100000000", "4.2", 120);
        when(loanMapper.findNormalLoansByUserId(7L)).thenReturn(List.of(target));

        var baseline = service.baseline(7L);
        var actual = service.forLever(7L, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, new BigDecimal("24"));
        var expected = calculationFor(target, 24, BigDecimal.ZERO);
        assertThat(actual.monthlyBurden()).isEqualByComparingTo(expected.monthlyPayment());
        assertThat(actual.totalInterest()).isEqualByComparingTo(expected.totalInterest());
        assertThat(actual.monthlyBurden()).isLessThan(baseline.monthlyBurden());
        assertThat(actual.totalInterest()).isGreaterThan(baseline.totalInterest());
        assertThat(actual.repaymentPeriodMonths()).isEqualTo(144);
    }

    @Test
    void income_change_keeps_loan_summary_unchanged() {
        when(loanMapper.findNormalLoansByUserId(7L)).thenReturn(List.of(loan("100000000", "4.2", 120)));
        var baseline = service.baseline(7L);
        var actual = service.forLever(7L, LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("1000000"));
        assertThat(actual.monthlyBurden()).isEqualByComparingTo(baseline.monthlyBurden().subtract(new BigDecimal("1000000")));
        assertThat(actual.totalInterest()).isEqualByComparingTo(baseline.totalInterest());
        assertThat(actual.repaymentPeriodMonths()).isEqualTo(baseline.repaymentPeriodMonths());
    }

    private StressTestLoanRow loan(String balance, String rate, int months) {
        StressTestLoanRow row = new StressTestLoanRow();
        row.setCurrentBalance(new BigDecimal(balance));
        row.setInterestRate(new BigDecimal(rate));
        row.setRepaymentType("원리금균등");
        row.setMaturityAt(LocalDate.now().plusMonths(months));
        return row;
    }

    private RepaymentCalculationResult calculationFor(StressTestLoanRow loan, int addedMonths, BigDecimal prepaid) {
        int months = calculator.calculateRemainingMonths(loan.getMaturityAt()) + addedMonths;
        return calculator.calculate(loan.getCurrentBalance().subtract(prepaid), loan.getInterestRate(), months, loan.getRepaymentType());
    }
}
