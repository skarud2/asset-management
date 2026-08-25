package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.dto.request.StressTestRequest;
import com.via.shinvia.stresstest.dto.response.StressTestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalStressTestSimulatorTest {

    @Mock
    private LoanBurdenAggregator loanBurdenAggregator;

    @Mock
    private LiquidAssetAggregator liquidAssetAggregator;

    @Mock
    private LivingExpenseEstimator livingExpenseEstimator;

    @Mock
    private AnnualIncomeProvider annualIncomeProvider;

    private PersonalStressTestSimulator simulator() {
        return new PersonalStressTestSimulator(
                loanBurdenAggregator, liquidAssetAggregator, livingExpenseEstimator, annualIncomeProvider
        );
    }

    @Test
    void 연소득_유동자산_데이터가_없으면_runway는_계산불가로_표시한다() {
        when(loanBurdenAggregator.aggregate(1L, new BigDecimal("1.0")))
                .thenReturn(new LoanBurdenAggregator.Result(new BigDecimal("1200000"), new BigDecimal("970000")));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));
        when(liquidAssetAggregator.aggregate(1L))
                .thenReturn(new LiquidAssetAggregator.Result(false, null));
        when(annualIncomeProvider.findAnnualIncome(1L))
                .thenReturn(new AnnualIncomeProvider.Result(false, null));

        StressTestResponse response = simulator().simulate(
                new StressTestRequest(1L, new BigDecimal("1.0"), new BigDecimal("20"), BigDecimal.ZERO, 24)
        );

        assertThat(response.runwayCalculable()).isFalse();
        assertThat(response.runwayMonths()).isNull();
        assertThat(response.totalStressedLoanPayment()).isEqualByComparingTo("1200000");
        assertThat(response.monthlyLivingExpense()).isEqualByComparingTo("2000000");
        assertThat(response.isLiquidAssetDataAvailable()).isFalse();
        assertThat(response.isIncomeDataAvailable()).isFalse();
        assertThat(response.timeline()).isEmpty();
    }

    @Test
    void 순현금흐름이_양수면_runwayMonths는_null이고_무기한_안전으로_계산된다() {
        when(loanBurdenAggregator.aggregate(1L, new BigDecimal("1.0")))
                .thenReturn(new LoanBurdenAggregator.Result(new BigDecimal("1000000"), new BigDecimal("900000")));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("1000000"), true, "disclaimer"));
        when(liquidAssetAggregator.aggregate(1L))
                .thenReturn(new LiquidAssetAggregator.Result(true, new BigDecimal("5000000")));
        when(annualIncomeProvider.findAnnualIncome(1L))
                .thenReturn(new AnnualIncomeProvider.Result(true, new BigDecimal("60000000")));

        StressTestResponse response = simulator().simulate(
                new StressTestRequest(1L, new BigDecimal("1.0"), new BigDecimal("10"), BigDecimal.ZERO, 24)
        );

        // monthlyIncome=5,000,000 * (1-0.10) = 4,500,000 - 1,000,000(대출) - 1,000,000(지출) = 2,500,000 (양수)
        assertThat(response.runwayCalculable()).isTrue();
        assertThat(response.netMonthlyCashflow()).isEqualByComparingTo("2500000");
        assertThat(response.runwayMonths()).isNull();
        assertThat(response.availableAssets()).isEqualByComparingTo("5000000");
        assertThat(response.timeline()).hasSize(25);
        assertThat(response.timeline().get(0).balance()).isEqualByComparingTo("5000000");
        // 현금흐름이 양수라 잔액이 계속 증가해야 함
        assertThat(response.timeline().get(1).balance().doubleValue())
                .isGreaterThan(response.timeline().get(0).balance().doubleValue());
    }

    @Test
    void 순현금흐름이_음수면_유동자산이_바닥나는_개월수를_계산한다() {
        when(loanBurdenAggregator.aggregate(1L, new BigDecimal("1.0")))
                .thenReturn(new LoanBurdenAggregator.Result(new BigDecimal("2000000"), new BigDecimal("1800000")));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("1500000"), true, "disclaimer"));
        when(liquidAssetAggregator.aggregate(1L))
                .thenReturn(new LiquidAssetAggregator.Result(true, new BigDecimal("3000000")));
        when(annualIncomeProvider.findAnnualIncome(1L))
                .thenReturn(new AnnualIncomeProvider.Result(true, new BigDecimal("48000000")));

        StressTestResponse response = simulator().simulate(
                new StressTestRequest(1L, new BigDecimal("1.0"), new BigDecimal("20"), BigDecimal.ZERO, 24)
        );

        // monthlyIncome=4,000,000 * 0.8 = 3,200,000 - 2,000,000 - 1,500,000 = -300,000
        assertThat(response.netMonthlyCashflow()).isEqualByComparingTo("-300000");
        // availableAssets(3,000,000) / 300,000 = 10개월
        assertThat(response.runwayMonths()).isEqualTo(10);
        // baseline: monthlyIncome(4,000,000, 드롭 없음) - totalCurrentLoanPayment(1,800,000) - 1,500,000 = 700,000 (양수) -> null
        assertThat(response.baselineRunwayMonths()).isNull();
        assertThat(response.timeline().get(10).balance()).isEqualByComparingTo("0");
    }

    @Test
    void 대출_카드_데이터가_전혀_없는_신규가입_사용자도_에러없이_처리된다() {
        when(loanBurdenAggregator.aggregate(2L, new BigDecimal("1.0")))
                .thenReturn(new LoanBurdenAggregator.Result(BigDecimal.ZERO, BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(2L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(BigDecimal.ZERO, true, "disclaimer"));
        when(liquidAssetAggregator.aggregate(2L))
                .thenReturn(new LiquidAssetAggregator.Result(false, null));
        when(annualIncomeProvider.findAnnualIncome(2L))
                .thenReturn(new AnnualIncomeProvider.Result(false, null));

        StressTestResponse response = simulator().simulate(
                new StressTestRequest(2L, new BigDecimal("1.0"), new BigDecimal("20"), BigDecimal.ZERO, 24)
        );

        assertThat(response.totalStressedLoanPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.monthlyLivingExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.runwayCalculable()).isFalse();
    }
}
