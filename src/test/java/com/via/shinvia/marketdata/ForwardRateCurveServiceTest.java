package com.via.shinvia.marketdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForwardRateCurveServiceTest {

    @Mock
    private BondYieldCurveProvider bondYieldCurveProvider;

    private ForwardRateCurveService service() {
        return new ForwardRateCurveService(bondYieldCurveProvider);
    }

    @Test
    void 첫_구간의_선도금리는_해당_만기의_spot_rate_그대로다() {
        LocalDate asOfDate = LocalDate.of(2026, 8, 5);
        when(bondYieldCurveProvider.getYieldCurve(asOfDate)).thenReturn(List.of(
                new YieldCurvePoint(12, new BigDecimal("3.0")),
                new YieldCurvePoint(24, new BigDecimal("2.8"))
        ));

        List<ForwardRatePoint> forwardRates = service().calculateForwardRates(asOfDate);

        assertThat(forwardRates.get(0).monthOffset()).isEqualTo(12);
        assertThat(forwardRates.get(0).impliedRate()).isEqualByComparingTo("3.0");
    }

    @Test
    void 두번째_구간의_선도금리는_부트스트래핑_공식대로_계산된다() {
        // 1년물 3.0%, 2년물 2.8% -> f = [(1.028)^2 / (1.03)^1]^(1/1) - 1 ≈ 2.600%
        LocalDate asOfDate = LocalDate.of(2026, 8, 5);
        when(bondYieldCurveProvider.getYieldCurve(asOfDate)).thenReturn(List.of(
                new YieldCurvePoint(12, new BigDecimal("3.0")),
                new YieldCurvePoint(24, new BigDecimal("2.8"))
        ));

        List<ForwardRatePoint> forwardRates = service().calculateForwardRates(asOfDate);

        assertThat(forwardRates).hasSize(2);
        assertThat(forwardRates.get(1).monthOffset()).isEqualTo(24);
        assertThat(forwardRates.get(1).impliedRate().doubleValue()).isCloseTo(2.600, offset(0.001));
    }

    @Test
    void 만기_순서와_상관없이_정렬해서_계산한다() {
        LocalDate asOfDate = LocalDate.of(2026, 8, 5);
        when(bondYieldCurveProvider.getYieldCurve(asOfDate)).thenReturn(List.of(
                new YieldCurvePoint(24, new BigDecimal("2.8")),
                new YieldCurvePoint(12, new BigDecimal("3.0"))
        ));

        List<ForwardRatePoint> forwardRates = service().calculateForwardRates(asOfDate);

        assertThat(forwardRates.get(0).monthOffset()).isEqualTo(12);
        assertThat(forwardRates.get(1).monthOffset()).isEqualTo(24);
    }

    @Test
    void 데이터가_없으면_빈_리스트를_반환한다() {
        LocalDate asOfDate = LocalDate.of(2026, 8, 5);
        when(bondYieldCurveProvider.getYieldCurve(asOfDate)).thenReturn(List.of());

        List<ForwardRatePoint> forwardRates = service().calculateForwardRates(asOfDate);

        assertThat(forwardRates).isEmpty();
    }
}
