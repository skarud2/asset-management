package com.via.shinvia.stresstest.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBurdenAggregatorTest {

    @Mock
    private StressTestLoanMapper loanMapper;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();

    private LoanBurdenAggregator aggregator() {
        return new LoanBurdenAggregator(loanMapper, repaymentCalculator);
    }

    private StressTestLoanRow loan(String rateType, BigDecimal balance, BigDecimal rate) {
        StressTestLoanRow loan = new StressTestLoanRow();
        loan.setLoanAccountId(1L);
        loan.setLoanType("신용대출");
        loan.setCurrentBalance(balance);
        loan.setInterestRate(rate);
        loan.setRateType(rateType);
        loan.setRepaymentType("원리금균등");
        loan.setMaturityAt(LocalDate.now().plusMonths(120));
        loan.setLoanStatus("정상");
        return loan;
    }

    @Test
    void 변동금리_대출에만_금리_스트레스가_적용되고_고정금리는_그대로_계산된다() {
        StressTestLoanRow variable1 = loan("변동", new BigDecimal("50000000"), new BigDecimal("4.0"));
        StressTestLoanRow variable2 = loan("변동", new BigDecimal("30000000"), new BigDecimal("3.5"));
        StressTestLoanRow fixed = loan("고정", new BigDecimal("20000000"), new BigDecimal("3.0"));

        when(loanMapper.findNormalLoansByUserId(1L)).thenReturn(List.of(variable1, variable2, fixed));

        LoanBurdenAggregator.Result result = aggregator().aggregate(1L, new BigDecimal("1.0"));

        BigDecimal expectedStressed = repaymentCalculator.calculate(
                        variable1.getCurrentBalance(), new BigDecimal("5.0"), 120, "원리금균등")
                .monthlyPayment()
                .add(repaymentCalculator.calculate(
                                variable2.getCurrentBalance(), new BigDecimal("4.5"), 120, "원리금균등")
                        .monthlyPayment())
                .add(repaymentCalculator.calculate(
                                fixed.getCurrentBalance(), new BigDecimal("3.0"), 120, "원리금균등")
                        .monthlyPayment());

        BigDecimal expectedCurrent = repaymentCalculator.calculate(
                        variable1.getCurrentBalance(), new BigDecimal("4.0"), 120, "원리금균등")
                .monthlyPayment()
                .add(repaymentCalculator.calculate(
                                variable2.getCurrentBalance(), new BigDecimal("3.5"), 120, "원리금균등")
                        .monthlyPayment())
                .add(repaymentCalculator.calculate(
                                fixed.getCurrentBalance(), new BigDecimal("3.0"), 120, "원리금균등")
                        .monthlyPayment());

        assertThat(result.totalStressedLoanPayment()).isEqualByComparingTo(expectedStressed);
        assertThat(result.totalCurrentLoanPayment()).isEqualByComparingTo(expectedCurrent);
        // 변동금리분만 스트레스가 반영돼야 하므로, 두 합계는 달라야 한다
        assertThat(result.totalStressedLoanPayment()).isGreaterThan(result.totalCurrentLoanPayment());
    }

    @Test
    void 보유_대출이_없으면_0을_반환한다() {
        when(loanMapper.findNormalLoansByUserId(1L)).thenReturn(List.of());

        LoanBurdenAggregator.Result result = aggregator().aggregate(1L, new BigDecimal("1.0"));

        assertThat(result.totalStressedLoanPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalCurrentLoanPayment()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
