package com.via.shinvia.report.service;

import com.via.shinvia.futuresim.service.AppConfigService;
import com.via.shinvia.futuresim.service.FutureSimulationEngine;
import com.via.shinvia.futuresim.service.UserFinancialSnapshotService;
import com.via.shinvia.report.mapper.ReportSpendingMapper;
import com.via.shinvia.stresstest.dto.response.StressTestResponse;
import com.via.shinvia.stresstest.service.PersonalStressTestSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialScoreCalculatorTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserFinancialSnapshotService userFinancialSnapshotService;
    @Mock
    private FutureSimulationEngine futureSimulationEngine;
    @Mock
    private PersonalStressTestSimulator personalStressTestSimulator;
    @Mock
    private ReportSpendingMapper reportSpendingMapper;
    @Mock
    private AppConfigService appConfigService;

    private FinancialScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FinancialScoreCalculator(
                userFinancialSnapshotService, futureSimulationEngine, personalStressTestSimulator,
                reportSpendingMapper, appConfigService
        );

        lenient().when(appConfigService.getDecimal(eq("REPORT_SCORE_WEIGHT_DSR"))).thenReturn(new BigDecimal("30"));
        lenient().when(appConfigService.getDecimal(eq("REPORT_SCORE_WEIGHT_SAVINGS_RATE"))).thenReturn(new BigDecimal("25"));
        lenient().when(appConfigService.getDecimal(eq("REPORT_SCORE_WEIGHT_DEBT_RATIO"))).thenReturn(new BigDecimal("20"));
        lenient().when(appConfigService.getDecimal(eq("REPORT_SCORE_WEIGHT_VULNERABILITY"))).thenReturn(new BigDecimal("15"));
        lenient().when(appConfigService.getDecimal(eq("REPORT_SCORE_WEIGHT_SPENDING_STABILITY"))).thenReturn(new BigDecimal("10"));

        // 중간값 기본 스텁 — 개별 테스트가 필요한 지표만 덮어쓴다.
        lenient().when(userFinancialSnapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                new BigDecimal("50000000"), new BigDecimal("20000000"), new BigDecimal("10000000"),
                new BigDecimal("10000000"), new BigDecimal("500000"), new BigDecimal("5000000")
        ));
        lenient().when(futureSimulationEngine.calculateSavingsCapacity(USER_ID)).thenReturn(new FutureSimulationEngine.SavingsCapacity(
                new BigDecimal("4000000"), new BigDecimal("2000000"), new BigDecimal("500000"), new BigDecimal("800000")
        ));
        lenient().when(personalStressTestSimulator.simulate(any())).thenReturn(stressResponse(true, 6));
        lenient().when(reportSpendingMapper.findMonthlyTotalsByUserId(eq(USER_ID), anyInt()))
                .thenReturn(List.of(new BigDecimal("1000000"), new BigDecimal("1000000"), new BigDecimal("1000000")));
    }

    private StressTestResponse stressResponse(boolean runwayCalculable, Integer runwayMonths) {
        return new StressTestResponse(runwayCalculable, null, runwayMonths, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true, BigDecimal.ZERO, BigDecimal.ZERO, false, null, true, BigDecimal.ZERO, List.of());
    }

    private BigDecimal dimensionScore(FinancialScoreCalculator.Result result, String key) {
        return result.dimensions().stream()
                .filter(d -> d.key().equals(key))
                .findFirst().orElseThrow()
                .score();
    }

    @Test
    void DSR가_0퍼센트면_100점이다() {
        when(userFinancialSnapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                new BigDecimal("50000000"), new BigDecimal("20000000"), new BigDecimal("10000000"),
                new BigDecimal("10000000"), new BigDecimal("500000"), BigDecimal.ZERO
        ));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "DSR");

        assertThat(score).isEqualByComparingTo("100");
    }

    @Test
    void DSR가_40퍼센트_이상이면_0점이다() {
        when(userFinancialSnapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                new BigDecimal("50000000"), new BigDecimal("20000000"), new BigDecimal("10000000"),
                new BigDecimal("10000000"), new BigDecimal("500000"), new BigDecimal("20000000")
        ));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "DSR");

        assertThat(score).isEqualByComparingTo("0");
    }

    @Test
    void 소득_정보가_없으면_DSR은_중립_50점이다() {
        when(userFinancialSnapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                null, new BigDecimal("20000000"), new BigDecimal("10000000"),
                new BigDecimal("10000000"), new BigDecimal("500000"), new BigDecimal("5000000")
        ));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "DSR");

        assertThat(score).isEqualByComparingTo("50");
    }

    @Test
    void 저축률이_30퍼센트_이상이면_100점이다() {
        when(futureSimulationEngine.calculateSavingsCapacity(USER_ID)).thenReturn(new FutureSimulationEngine.SavingsCapacity(
                new BigDecimal("4000000"), new BigDecimal("1600000"), new BigDecimal("400000"), new BigDecimal("2000000")
        ));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "SAVINGS_RATE");

        assertThat(score).isEqualByComparingTo("100");
    }

    @Test
    void 저축률이_0퍼센트_이하이면_0점이다() {
        when(futureSimulationEngine.calculateSavingsCapacity(USER_ID)).thenReturn(new FutureSimulationEngine.SavingsCapacity(
                new BigDecimal("4000000"), new BigDecimal("4000000"), new BigDecimal("0"), BigDecimal.ZERO
        ));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "SAVINGS_RATE");

        assertThat(score).isEqualByComparingTo("0");
    }

    @Test
    void 런웨이가_12개월_이상이면_취약도역산_100점이다() {
        when(personalStressTestSimulator.simulate(any())).thenReturn(stressResponse(true, 12));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "VULNERABILITY");

        assertThat(score).isEqualByComparingTo("100");
    }

    @Test
    void 런웨이_계산_불가면_취약도역산은_중립_50점이다() {
        when(personalStressTestSimulator.simulate(any())).thenReturn(stressResponse(false, null));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "VULNERABILITY");

        assertThat(score).isEqualByComparingTo("50");
    }

    @Test
    void 최근_지출이_전부_동일하면_소비안정성_100점이다() {
        when(reportSpendingMapper.findMonthlyTotalsByUserId(eq(USER_ID), anyInt()))
                .thenReturn(List.of(new BigDecimal("1000000"), new BigDecimal("1000000")));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "SPENDING_STABILITY");

        assertThat(score).isEqualByComparingTo("100");
    }

    @Test
    void 지출_데이터가_2개월_미만이면_소비안정성은_중립_50점이다() {
        when(reportSpendingMapper.findMonthlyTotalsByUserId(eq(USER_ID), anyInt()))
                .thenReturn(List.of(new BigDecimal("1000000")));

        BigDecimal score = dimensionScore(calculator.calculate(USER_ID), "SPENDING_STABILITY");

        assertThat(score).isEqualByComparingTo("50");
    }

    @Test
    void 종합점수는_5개_지표의_가중평균이다() {
        FinancialScoreCalculator.Result result = calculator.calculate(USER_ID);

        BigDecimal expected = result.dimensions().stream()
                .map(d -> d.score().multiply(d.weightPercent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("100"), 1, java.math.RoundingMode.HALF_UP);

        assertThat(result.totalScore()).isEqualByComparingTo(expected);
    }

    @Test
    void 종합점수에_부채비율을_포함한_5개_지표를_항상_반환한다() {
        FinancialScoreCalculator.Result result = calculator.calculate(USER_ID);

        assertThat(result.dimensions())
                .extracting(FinancialScoreCalculator.DimensionScore::key)
                .containsExactly("DSR", "SAVINGS_RATE", "DEBT_RATIO", "VULNERABILITY", "SPENDING_STABILITY");
    }
}
