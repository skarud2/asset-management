package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestFinancialProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiquidAssetAggregatorTest {

    @Mock
    private StressTestFinancialProfileMapper financialProfileMapper;

    private LiquidAssetAggregator aggregator() {
        return new LiquidAssetAggregator(financialProfileMapper);
    }

    @Test
    void 재무_프로필이_있으면_유동자산_금액을_그대로_반환한다() {
        when(financialProfileMapper.findLiquidAssetAmountByUserId(1L))
                .thenReturn(new BigDecimal("10000000"));

        LiquidAssetAggregator.Result result = aggregator().aggregate(1L);

        assertThat(result.available()).isTrue();
        assertThat(result.totalLiquidAssets()).isEqualByComparingTo("10000000");
    }

    @Test
    void 재무_프로필이_없으면_데이터_없음으로_반환한다() {
        when(financialProfileMapper.findLiquidAssetAmountByUserId(2L)).thenReturn(null);

        LiquidAssetAggregator.Result result = aggregator().aggregate(2L);

        assertThat(result.available()).isFalse();
        assertThat(result.totalLiquidAssets()).isNull();
    }
}
