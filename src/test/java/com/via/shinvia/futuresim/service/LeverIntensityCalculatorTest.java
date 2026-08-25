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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeverIntensityCalculatorTest {

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

    // 순수 계산 로직(DB 조회 없음)이라 실제 구현을 그대로 쓴다 — loan/ratesimulation은 읽기 전용 재사용.
    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();

    private LeverIntensityCalculator calculator() {
        FutureSimulationEngine engine = new FutureSimulationEngine(snapshotService, livingExpenseEstimator, appConfigService);
        return new LeverIntensityCalculator(engine, loanMapper, repaymentCalculator);
    }

    // 대출 없이 소득 6천만원/연, 생활비 2백만원/월인 기본 스냅샷 — 월 저축여력 3백만원.
    private void stubBaseSnapshot() {
        when(snapshotService.getSnapshot(USER_ID)).thenReturn(new UserFinancialSnapshotService.Snapshot(
                new BigDecimal("60000000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null
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
    void 소득_변화_레버는_강도가_커질수록_단축_개월수가_단조_증가한다() {
        stubBaseSnapshot();

        LeverIntensityCalculator.IntensityCurve curve =
                calculator().calculateIntensityCurve(USER_ID, GOAL_AMOUNT, LeverIntensityCalculator.LeverType.INCOME_CHANGE);

        List<Integer> diffs = curve.points().stream().map(LeverIntensityCalculator.IntensityPoint::diffMonths).toList();
        assertThat(diffs).doesNotContainNull();
        for (int i = 1; i < diffs.size(); i++) {
            assertThat(diffs.get(i)).isGreaterThanOrEqualTo(diffs.get(i - 1));
        }
        assertThat(diffs.get(diffs.size() - 1)).isGreaterThan(diffs.get(0));
    }

    @Test
    void 월_추가_확보_빠른선택값은_서로_다른_원_단위_금액이다() {
        assertThat(calculator().presetIntensitiesFor(USER_ID, LeverIntensityCalculator.LeverType.INCOME_CHANGE))
                .containsExactly(new BigDecimal("100000"), new BigDecimal("500000"), new BigDecimal("1000000"));
    }

    @Test
    void 신규_대출_레버는_강도가_커질수록_지연_개월수가_단조_감소하고_결국_예측_불가로_수렴한다() {
        stubBaseSnapshot();

        LeverIntensityCalculator.IntensityCurve curve =
                calculator().calculateIntensityCurve(USER_ID, GOAL_AMOUNT, LeverIntensityCalculator.LeverType.NEW_LOAN);

        List<Integer> diffs = curve.points().stream().map(LeverIntensityCalculator.IntensityPoint::diffMonths).toList();

        int lastFiniteIndex = -1;
        for (int i = 0; i < diffs.size(); i++) {
            if (diffs.get(i) != null) {
                lastFiniteIndex = i;
            }
        }
        assertThat(lastFiniteIndex).isGreaterThanOrEqualTo(0);

        for (int i = 1; i <= lastFiniteIndex; i++) {
            if (diffs.get(i) != null && diffs.get(i - 1) != null) {
                assertThat(diffs.get(i)).isLessThanOrEqualTo(diffs.get(i - 1));
            }
        }
        assertThat(diffs.get(lastFiniteIndex)).isLessThan(diffs.get(0));

        // 한 번 도달 불가(null)가 되면 그보다 강도가 큰 지점도 계속 도달 불가여야 한다(다시 finite로 돌아오지 않음).
        for (int i = lastFiniteIndex + 1; i < diffs.size(); i++) {
            assertThat(diffs.get(i)).isNull();
        }
    }

    @Test
    void 대출이_없으면_조기상환과_만기연장_레버는_이용_불가다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of());

        LeverIntensityCalculator calculator = calculator();

        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT)).isFalse();
        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION)).isFalse();
        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.INCOME_CHANGE)).isTrue();
        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.NEW_LOAN)).isTrue();
    }

    @Test
    void 대출이_있으면_조기상환과_만기연장_레버를_이용할_수_있고_강도_범위는_대출_잔액으로_제한된다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverIntensityCalculator calculator = calculator();

        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT)).isTrue();
        assertThat(calculator.isLeverAvailable(USER_ID, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION)).isTrue();

        List<BigDecimal> prepaymentPoints = calculator.intensityPointsFor(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT);
        assertThat(prepaymentPoints.get(prepaymentPoints.size() - 1)).isEqualByComparingTo("80000000");
    }

    @Test
    void 강도_칩_프리셋은_최소_기본_최대_강도로_구성된다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        List<BigDecimal> presets = calculator().presetIntensitiesFor(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT);

        assertThat(presets).hasSize(3);
        assertThat(presets.get(0)).isEqualByComparingTo("1000000");
        assertThat(presets.get(1)).isEqualByComparingTo("5000000");
        assertThat(presets.get(2)).isEqualByComparingTo("80000000");
    }

    @Test
    void 대출_잔액이_작아도_최소_기본_최대_강도_프리셋을_제공한다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("30000000"))));

        List<BigDecimal> presets = calculator().presetIntensitiesFor(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT);

        assertThat(presets).hasSize(3);
        assertThat(presets.get(0)).isEqualByComparingTo("1000000");
        assertThat(presets.get(1)).isEqualByComparingTo("5000000");
        assertThat(presets.get(2)).isEqualByComparingTo("30000000");
    }

    @Test
    void 조기상환_상세는_상환_전후_월상환액을_보여준다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverIntensityCalculator.LeverDetail detail =
                calculator().detailFor(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, new BigDecimal("50000000"));

        assertThat(detail.beforeMonthlyPayment()).isNotNull();
        assertThat(detail.afterMonthlyPayment()).isNotNull();
        assertThat(detail.afterMonthlyPayment()).isLessThan(detail.beforeMonthlyPayment());
    }

    @Test
    void 만기연장_상세는_월상환액은_줄지만_총이자는_늘어난다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));

        LeverIntensityCalculator.LeverDetail detail =
                calculator().detailFor(USER_ID, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, new BigDecimal("60"));

        assertThat(detail.afterMonthlyPayment()).isLessThan(detail.beforeMonthlyPayment());
        assertThat(detail.extraTotalInterest()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void 신규대출_상세는_새로_생기는_월상환액을_보여준다() {
        LeverIntensityCalculator.LeverDetail detail =
                calculator().detailFor(USER_ID, LeverIntensityCalculator.LeverType.NEW_LOAN, new BigDecimal("100000000"));

        assertThat(detail.afterMonthlyPayment()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void 소득변화는_상세값이_없다() {
        LeverIntensityCalculator.LeverDetail detail =
                calculator().detailFor(USER_ID, LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("20"));

        assertThat(detail.beforeMonthlyPayment()).isNull();
        assertThat(detail.afterMonthlyPayment()).isNull();
    }

    // ==== 5단계("레버 조합해보기") — resolveCombinedAdjustment의 상호작용 ====

    @Test
    void 조기상환과_만기연장을_함께_선택하면_단순_합산이_아닌_실제_상호작용이_반영된다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));
        LeverIntensityCalculator calculator = calculator();

        BigDecimal prepay = new BigDecimal("30000000");
        BigDecimal extension = new BigDecimal("60");

        // 두 레버를 각각 독립적으로 계산해서 단순히 더한 값(잘못된 방식이라면 이렇게 나올 것).
        FutureSimulationEngine.Adjustment prepayOnly =
                calculator.resolveAdjustment(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, prepay);
        FutureSimulationEngine.Adjustment extensionOnly =
                calculator.resolveAdjustment(USER_ID, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, extension);
        BigDecimal naiveSumCashFlow = prepayOnly.monthlyCashFlowDelta().add(extensionOnly.monthlyCashFlowDelta());

        // 실제 조합(같은 대출에 조기상환으로 줄어든 잔액 기준으로 만기연장까지 반영).
        FutureSimulationEngine.Adjustment combined = calculator.resolveCombinedAdjustment(USER_ID, List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, prepay),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, extension)
        ));

        assertThat(combined.monthlyCashFlowDelta()).isNotEqualByComparingTo(naiveSumCashFlow);
    }

    @Test
    void 만기연장만_선택하면_조합_계산도_단일_레버_계산과_같은_값을_낸다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));
        LeverIntensityCalculator calculator = calculator();
        BigDecimal extension = new BigDecimal("60");

        FutureSimulationEngine.Adjustment single =
                calculator.resolveAdjustment(USER_ID, LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, extension);
        FutureSimulationEngine.Adjustment combined = calculator.resolveCombinedAdjustment(USER_ID, List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION, extension)
        ));

        assertThat(combined.monthlyCashFlowDelta()).isEqualByComparingTo(single.monthlyCashFlowDelta());
    }

    @Test
    void 소득변화와_조기상환처럼_서로_다른_대출을_건드리지_않는_레버는_단순_합산과_같다() {
        when(loanMapper.findNormalLoansByUserId(USER_ID)).thenReturn(List.of(loan(new BigDecimal("80000000"))));
        LeverIntensityCalculator calculator = calculator();

        BigDecimal monthlyExtraCapacity = new BigDecimal("500000");
        BigDecimal prepay = new BigDecimal("30000000");

        FutureSimulationEngine.Adjustment incomeOnly =
                calculator.resolveAdjustment(USER_ID, LeverIntensityCalculator.LeverType.INCOME_CHANGE, monthlyExtraCapacity);
        FutureSimulationEngine.Adjustment prepayOnly =
                calculator.resolveAdjustment(USER_ID, LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, prepay);
        BigDecimal naiveSum = incomeOnly.monthlyCashFlowDelta().add(prepayOnly.monthlyCashFlowDelta());

        FutureSimulationEngine.Adjustment combined = calculator.resolveCombinedAdjustment(USER_ID, List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.INCOME_CHANGE, monthlyExtraCapacity),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, prepay)
        ));

        assertThat(combined.monthlyCashFlowDelta()).isEqualByComparingTo(naiveSum);
    }

    @Test
    void 단일_강도_조회와_전체_곡선_조회는_같은_강도에서_같은_값을_반환한다() {
        stubBaseSnapshot();
        LeverIntensityCalculator calculator = calculator();

        LeverIntensityCalculator.IntensityCurve curve =
                calculator.calculateIntensityCurve(USER_ID, GOAL_AMOUNT, LeverIntensityCalculator.LeverType.INCOME_CHANGE);
        LeverIntensityCalculator.IntensityPoint gridPoint = curve.points().stream()
                .filter(point -> point.intensity().compareTo(new BigDecimal("500000")) == 0)
                .findFirst()
                .orElseThrow();

        Integer singlePointDiff = calculator.calculateDiffMonths(
                USER_ID, GOAL_AMOUNT, LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("500000")
        );

        assertThat(singlePointDiff).isEqualTo(gridPoint.diffMonths());
    }

    @Test
    void 효과_둔화_지점은_직전_구간_대비_증분이_30퍼센트_이하로_떨어지는_첫_지점이다() {
        // 증분: 10, 10, 8, 2 -> 세 번째 구간(2/8=0.25)에서 처음 30% 이하로 떨어진다.
        List<LeverIntensityCalculator.IntensityPoint> points = intensityPoints(
                new int[]{1, 2, 3, 4, 5}, new Integer[]{0, 10, 20, 28, 30}
        );

        BigDecimal diminishingIntensity = calculator().findDiminishingReturnIntensity(points);

        assertThat(diminishingIntensity).isEqualByComparingTo("4");
    }

    @Test
    void 증분이_계속_유지되면_효과_둔화_지점이_없다() {
        List<LeverIntensityCalculator.IntensityPoint> points = intensityPoints(
                new int[]{1, 2, 3, 4, 5}, new Integer[]{0, 10, 20, 30, 40}
        );

        assertThat(calculator().findDiminishingReturnIntensity(points)).isNull();
    }

    @Test
    void 도달_불가_구간이_섞여있어도_null끼리는_증분_비교에서_건너뛴다() {
        List<LeverIntensityCalculator.IntensityPoint> points = intensityPoints(
                new int[]{1, 2, 3, 4}, new Integer[]{0, 10, null, 30}
        );

        assertThat(calculator().findDiminishingReturnIntensity(points)).isNull();
    }

    private List<LeverIntensityCalculator.IntensityPoint> intensityPoints(int[] intensities, Integer[] diffMonths) {
        List<LeverIntensityCalculator.IntensityPoint> points = new ArrayList<>();
        for (int i = 0; i < intensities.length; i++) {
            points.add(new LeverIntensityCalculator.IntensityPoint(BigDecimal.valueOf(intensities[i]), diffMonths[i]));
        }
        return points;
    }
}
