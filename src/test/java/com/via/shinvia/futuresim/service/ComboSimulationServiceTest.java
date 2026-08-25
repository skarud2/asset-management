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
class ComboSimulationServiceTest {

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

    private ComboSimulationService service() {
        FutureSimulationEngine engine = new FutureSimulationEngine(snapshotService, livingExpenseEstimator, appConfigService);
        LeverIntensityCalculator leverCalculator = new LeverIntensityCalculator(engine, loanMapper, repaymentCalculator);
        return new ComboSimulationService(engine, leverCalculator);
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
    void 레버를_하나도_선택하지_않으면_조합_결과는_baseline과_같다() {
        stubSnapshot(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("500000"));

        ComboSimulationService.ComboResult result = service().simulate(USER_ID, GOAL_AMOUNT, List.of());

        assertThat(result.comboMonthsToGoal()).isEqualTo(result.baselineMonthsToGoal());
        assertThat(result.diffMonths()).isZero();
    }

    @Test
    void 레버를_조합하면_baseline보다_같거나_빠르다() {
        stubSnapshot(new BigDecimal("100000000"), new BigDecimal("80000000"), new BigDecimal("1500000"));
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        List<LeverIntensityCalculator.LeverSelection> selections = List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, new BigDecimal("30000000")),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, new BigDecimal("60")),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("20"))
        );

        ComboSimulationService.ComboResult result = service().simulate(USER_ID, GOAL_AMOUNT, selections);

        assertThat(result.baselineMonthsToGoal()).isNotNull();
        assertThat(result.comboMonthsToGoal()).isNotNull();
        assertThat(result.comboMonthsToGoal()).isLessThanOrEqualTo(result.baselineMonthsToGoal());
        assertThat(result.timeline()).isNotEmpty();
        assertThat(result.timeline().get(0).monthOffset()).isZero();
    }
}
