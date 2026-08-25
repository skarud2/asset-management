package com.via.shinvia.loan.ratesimulation.historical.service;

import com.via.shinvia.client.ecos.EcosApiClient;
import com.via.shinvia.client.ecos.EcosDailyRate;
import com.via.shinvia.loan.ratesimulation.historical.dto.response.RateChangePoint;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HistoricalRateReplaySimulatorTest {

    @Mock
    private LoanAccountSummaryMapper loanAccountSummaryMapper;

    @Mock
    private EcosApiClient ecosApiClient;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();
    private final StagedRateSimulator stagedRateSimulator = new StagedRateSimulator(loanAccountSummaryMapper, repaymentCalculator);

    private final HistoricalRateReplaySimulator simulator =
            new HistoricalRateReplaySimulator(loanAccountSummaryMapper, repaymentCalculator, stagedRateSimulator, ecosApiClient);

    @Test
    void 값이_바뀐_날짜만_변경점으로_추출하고_첫날은_무조건_포함한다() {
        List<EcosDailyRate> dailySeries = List.of(
                new EcosDailyRate(LocalDate.of(2024, 1, 1), new BigDecimal("0.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 2), new BigDecimal("0.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 3), new BigDecimal("0.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 4), new BigDecimal("0.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 5), new BigDecimal("0.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 6), new BigDecimal("0.75")),
                new EcosDailyRate(LocalDate.of(2024, 1, 7), new BigDecimal("0.75")),
                new EcosDailyRate(LocalDate.of(2024, 1, 8), new BigDecimal("0.75")),
                new EcosDailyRate(LocalDate.of(2024, 1, 9), new BigDecimal("0.75"))
        );

        List<RateChangePoint> changePoints = simulator.extractRateChangePoints(dailySeries);

        assertThat(changePoints).hasSize(2);

        assertThat(changePoints.get(0).date()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(changePoints.get(0).newRate()).isEqualByComparingTo("0.50");
        assertThat(changePoints.get(0).changeBp()).isZero();

        assertThat(changePoints.get(1).date()).isEqualTo(LocalDate.of(2024, 1, 6));
        assertThat(changePoints.get(1).newRate()).isEqualByComparingTo("0.75");
        assertThat(changePoints.get(1).changeBp()).isEqualTo(25);
    }

    @Test
    void 금리_인하_구간은_음수_changeBp로_추출된다() {
        List<EcosDailyRate> dailySeries = List.of(
                new EcosDailyRate(LocalDate.of(2024, 1, 1), new BigDecimal("3.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 2), new BigDecimal("3.25")),
                new EcosDailyRate(LocalDate.of(2024, 1, 3), new BigDecimal("3.25"))
        );

        List<RateChangePoint> changePoints = simulator.extractRateChangePoints(dailySeries);

        assertThat(changePoints).hasSize(2);
        assertThat(changePoints.get(1).changeBp()).isEqualTo(-25);
    }

    @Test
    void 변동이_전혀_없으면_시작점_1개만_추출된다() {
        List<EcosDailyRate> dailySeries = List.of(
                new EcosDailyRate(LocalDate.of(2024, 1, 1), new BigDecimal("3.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 2), new BigDecimal("3.50")),
                new EcosDailyRate(LocalDate.of(2024, 1, 3), new BigDecimal("3.50"))
        );

        List<RateChangePoint> changePoints = simulator.extractRateChangePoints(dailySeries);

        assertThat(changePoints).hasSize(1);
        assertThat(changePoints.get(0).changeBp()).isZero();
    }

    @Test
    void 빈_시계열이면_빈_리스트를_반환한다() {
        List<RateChangePoint> changePoints = simulator.extractRateChangePoints(List.of());

        assertThat(changePoints).isEmpty();
    }
}
