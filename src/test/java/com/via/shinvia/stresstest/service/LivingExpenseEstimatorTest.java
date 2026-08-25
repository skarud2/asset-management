package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestCardTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivingExpenseEstimatorTest {

    @Mock
    private StressTestCardTransactionMapper cardTransactionMapper;

    private LivingExpenseEstimator estimator() {
        return new LivingExpenseEstimator(cardTransactionMapper);
    }

    @Test
    void 최근_3개월_카드_이용금액_합계를_3으로_나눈_값이_월평균_지출이다() {
        when(cardTransactionMapper.sumAmountByUserIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("6000000"));

        LivingExpenseEstimator.Result result = estimator().estimate(1L, 3);

        assertThat(result.monthlyLivingExpense()).isEqualByComparingTo("2000000.00");
        assertThat(result.isEstimate()).isTrue();
        assertThat(result.disclaimer()).contains("카드 거래내역만");
    }

    @Test
    void 거래내역이_없으면_0을_반환한다() {
        when(cardTransactionMapper.sumAmountByUserIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        LivingExpenseEstimator.Result result = estimator().estimate(1L, 3);

        assertThat(result.monthlyLivingExpense()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void lookbackMonths가_0_이하이면_예외를_던진다() {
        assertThatThrownBy(() -> estimator().estimate(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
