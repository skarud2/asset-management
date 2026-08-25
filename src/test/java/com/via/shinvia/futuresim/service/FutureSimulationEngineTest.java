package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.service.LivingExpenseEstimator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutureSimulationEngineTest {

    @Mock
    private UserFinancialSnapshotService snapshotService;

    @Mock
    private LivingExpenseEstimator livingExpenseEstimator;

    @Mock
    private AppConfigService appConfigService;

    private FutureSimulationEngine engine() {
        return new FutureSimulationEngine(snapshotService, livingExpenseEstimator, appConfigService);
    }

    private UserFinancialSnapshotService.Snapshot snapshot(BigDecimal netWorth, BigDecimal annualIncome, BigDecimal monthlyLoanPayment) {
        return new UserFinancialSnapshotService.Snapshot(
                annualIncome, null, null, netWorth, monthlyLoanPayment, null
        );
    }

    // 3단계(성장 곡선)용 — liquidAsset/totalDebt를 직접 채운 스냅샷. netWorth는 둘의 차로 계산해 넣는다.
    private UserFinancialSnapshotService.Snapshot snapshotWithAssets(
            BigDecimal liquidAsset, BigDecimal totalDebt, BigDecimal annualIncome, BigDecimal monthlyLoanPayment
    ) {
        return new UserFinancialSnapshotService.Snapshot(
                annualIncome, liquidAsset, totalDebt, liquidAsset.subtract(totalDebt), monthlyLoanPayment, null
        );
    }

    @Test
    void 이미_순자산이_목표금액_이상이면_0개월이다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshot(new BigDecimal("300000000"), BigDecimal.ZERO, BigDecimal.ZERO));

        Integer months = engine().calculateMonthsToGoal(1L, new BigDecimal("200000000"));

        assertThat(months).isZero();
    }

    @Test
    void 매달_저축액만큼_순자산이_늘어나서_목표까지_걸리는_개월수를_계산한다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshot(new BigDecimal("0"), new BigDecimal("48000000"), BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));

        Integer months = engine().calculateMonthsToGoal(1L, new BigDecimal("24000000"));

        assertThat(months).isEqualTo(12);
    }

    @Test
    void 월_저축액이_0_이하이면_null을_반환한다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshot(new BigDecimal("0"), new BigDecimal("24000000"), BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("3000000"), true, "disclaimer"));

        Integer months = engine().calculateMonthsToGoal(1L, new BigDecimal("100000000"));

        assertThat(months).isNull();
    }

    @Test
    void 도달까지_100년을_초과하면_null을_반환한다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshot(new BigDecimal("0"), new BigDecimal("120000"), BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(BigDecimal.ZERO, true, "disclaimer"));

        Integer months = engine().calculateMonthsToGoal(1L, new BigDecimal("1000000000"));

        assertThat(months).isNull();
    }

    @Test
    void 복리_반영_시_단순_덧셈_방식보다_목표_도달까지_걸리는_개월수가_같거나_짧다() {
        BigDecimal liquidAsset = new BigDecimal("10000000");
        BigDecimal totalDebt = BigDecimal.ZERO;
        BigDecimal annualIncome = new BigDecimal("48000000");
        BigDecimal goalAmount = new BigDecimal("50000000");

        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshotWithAssets(liquidAsset, totalDebt, annualIncome, BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));
        when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));

        Integer linearMonths = engine().calculateMonthsToGoal(1L, goalAmount);
        FutureSimulationEngine.Projection projection = engine().calculateProjection(1L, goalAmount);

        assertThat(linearMonths).isNotNull();
        assertThat(projection.monthsToGoal()).isNotNull();
        assertThat(projection.monthsToGoal()).isLessThanOrEqualTo(linearMonths);
    }

    @Test
    void 프로젝션_타임라인은_0개월_시점에_현재_순자산으로_시작한다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshotWithAssets(new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("36000000"), BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("1000000"), true, "disclaimer"));
        when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));

        FutureSimulationEngine.Projection projection = engine().calculateProjection(1L, new BigDecimal("100000000"));

        assertThat(projection.timeline().get(0).monthOffset()).isZero();
        assertThat(projection.timeline().get(0).netWorth()).isEqualByComparingTo("4000000");
    }

    @Test
    void 이백사십개월_안에_도달하지_못하면_crossoverReached가_false다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshotWithAssets(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("120000"), BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(BigDecimal.ZERO, true, "disclaimer"));
        when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));

        FutureSimulationEngine.Projection projection = engine().calculateProjection(1L, new BigDecimal("1000000000"));

        assertThat(projection.crossoverReached()).isFalse();
        assertThat(projection.monthsToGoal()).isNull();
        assertThat(projection.timeline()).hasSize(1201);
    }

    @Test
    void 이미_목표금액_이상이면_0개월이고_버퍼만큼만_타임라인이_이어진다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshotWithAssets(new BigDecimal("300000000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(BigDecimal.ZERO, true, "disclaimer"));
        when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));

        FutureSimulationEngine.Projection projection = engine().calculateProjection(1L, new BigDecimal("200000000"));

        assertThat(projection.monthsToGoal()).isZero();
        assertThat(projection.crossoverReached()).isTrue();
        assertThat(projection.timeline()).hasSize(13);
    }

    @Test
    void 모든_시점에서_원금과_투자수익의_합은_순자산과_정확히_일치한다() {
        when(snapshotService.getSnapshot(1L))
                .thenReturn(snapshotWithAssets(new BigDecimal("5000000"), new BigDecimal("2000000"), new BigDecimal("48000000"), new BigDecimal("300000")));
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));
        when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));

        FutureSimulationEngine.Projection projection = engine().calculateProjection(1L, new BigDecimal("100000000"));

        projection.timeline().forEach(point ->
                assertThat(point.contributionAmount().add(point.returnAmount()))
                        .isEqualByComparingTo(point.netWorth())
        );
    }
}
