package com.via.shinvia.futuresim.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import com.via.shinvia.stresstest.service.LivingExpenseEstimator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeverCombinationOptimizerTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("100000000");

    @Mock
    private UserFinancialSnapshotService snapshotService;

    @Mock
    private LivingExpenseEstimator livingExpenseEstimator;

    @Mock
    private AppConfigService appConfigService;

    @Mock
    private StressTestLoanMapper loanMapper;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();

    private LeverCombinationOptimizer optimizer() {
        FutureSimulationEngine engine = new FutureSimulationEngine(snapshotService, livingExpenseEstimator, appConfigService);
        LeverIntensityCalculator leverCalculator = new LeverIntensityCalculator(engine, loanMapper, repaymentCalculator);
        return new LeverCombinationOptimizer(engine, leverCalculator, snapshotService);
    }

    private void stubSnapshot(BigDecimal liquidAsset, BigDecimal totalDebt, BigDecimal monthlyLoanPayment) {
        when(snapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                new BigDecimal("60000000"), liquidAsset, totalDebt, liquidAsset.subtract(totalDebt), monthlyLoanPayment, null
        ));
        lenient().when(livingExpenseEstimator.estimate(USER_ID, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));
        lenient().when(appConfigService.getDecimal("FUTURESIM_ASSUMED_RETURN_RATE")).thenReturn(new BigDecimal("3.08"));
    }

    private StressTestLoanRow loan(BigDecimal balance) {
        StressTestLoanRow row = new StressTestLoanRow();
        row.setLoanAccountId(1L);
        row.setLoanType("MORTGAGE_LOAN");
        row.setCurrentBalance(balance);
        row.setInterestRate(new BigDecimal("4.2"));
        row.setRateType("고정");
        row.setRepaymentType("원리금균등");
        row.setMaturityAt(LocalDate.now().plusMonths(120));
        row.setLoanStatus("정상");
        return row;
    }

    @Test
    void 유동자산이_충분하면_조합이_baseline보다_같거나_빠르다() {
        stubSnapshot(new BigDecimal("100000000"), new BigDecimal("80000000"), new BigDecimal("1500000"));
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverCombinationOptimizer.CombinationResult result = optimizer().findOptimalCombination(USER_ID, GOAL_AMOUNT);

        assertThat(result.baselineMonths()).isNotNull();
        assertThat(result.combinedMonths()).isNotNull();
        assertThat(result.combinedMonths()).isLessThanOrEqualTo(result.baselineMonths());
        assertThat(result.chosenLevers()).isNotEmpty();
    }

    @Test
    void 유동자산이_최소_조기상환_강도보다_적으면_조기상환은_조합에서_제외된다() {
        stubSnapshot(new BigDecimal("5000000"), new BigDecimal("80000000"), new BigDecimal("1500000"));
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverCombinationOptimizer.CombinationResult result = optimizer().findOptimalCombination(USER_ID, GOAL_AMOUNT);

        assertThat(result.chosenLevers())
                .noneMatch(choice -> choice.leverType() == LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT);
    }

    @Test
    void 신규대출은_가정금리가_수익률보다_높아서_어떤_경우에도_조합에_포함되지_않는다() {
        stubSnapshot(new BigDecimal("100000000"), new BigDecimal("80000000"), new BigDecimal("1500000"));
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverCombinationOptimizer.CombinationResult result = optimizer().findOptimalCombination(USER_ID, GOAL_AMOUNT);

        assertThat(result.chosenLevers())
                .noneMatch(choice -> choice.leverType() == LeverIntensityCalculator.LeverType.NEW_LOAN);
    }

    @Test
    void 대출이_없으면_조기상환과_만기연장은_애초에_선택지에_없다() {
        stubSnapshot(new BigDecimal("100000000"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of());

        LeverCombinationOptimizer.CombinationResult result = optimizer().findOptimalCombination(USER_ID, GOAL_AMOUNT);

        assertThat(result.chosenLevers())
                .allMatch(choice -> choice.leverType() == LeverIntensityCalculator.LeverType.INCOME_CHANGE);
    }

    @Test
    void 조합에_포함된_강도는_실제_프리셋_값_중_하나다() {
        stubSnapshot(new BigDecimal("100000000"), new BigDecimal("80000000"), new BigDecimal("1500000"));
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));
        LeverIntensityCalculator leverCalculator = new LeverIntensityCalculator(
                new FutureSimulationEngine(snapshotService, livingExpenseEstimator, appConfigService), loanMapper, repaymentCalculator
        );

        LeverCombinationOptimizer.CombinationResult result = optimizer().findOptimalCombination(USER_ID, GOAL_AMOUNT);

        result.chosenLevers().forEach(choice -> {
            List<BigDecimal> presets = leverCalculator.presetIntensitiesFor(USER_ID, choice.leverType());
            assertThat(presets).anyMatch(preset -> preset.compareTo(choice.intensity()) == 0);
        });
    }
}
