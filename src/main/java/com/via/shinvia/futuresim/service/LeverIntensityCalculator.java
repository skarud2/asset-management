package com.via.shinvia.futuresim.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 4단계("레버 랭킹")용 — 레버 4종(소득 변화/조기상환/만기연장/신규대출)의 강도를 바꿔가며
// FutureSimulationEngine.calculateMonthsToGoalCompound()를 반복 실행해 "강도-효과" 곡선을 만든다.
// loan/ratesimulation/의 LoanRepaymentCalculator는 읽기 전용으로 재사용만 한다(수정 없음).
@Service
public class LeverIntensityCalculator {

    // NEW_LOAN 레버는 아직 실행하지 않은 가상의 신규 대출이라, 금리·기간·상환방식을 고정 가정값으로 둔다.
    // 화면에도 그대로 노출한다(사용자가 조정할 수 없는 값이라는 걸 명확히 하기 위해).
    public static final BigDecimal NEW_LOAN_ASSUMED_RATE_PERCENT = new BigDecimal("4.5");
    public static final int NEW_LOAN_ASSUMED_TERM_MONTHS = 60;
    public static final String NEW_LOAN_ASSUMED_REPAYMENT_TYPE = "원리금균등";

    private static final BigDecimal PREPAYMENT_MIN = new BigDecimal("1000000");
    private static final BigDecimal PREPAYMENT_MAX_CAP = new BigDecimal("150000000");
    private static final int TERM_EXTENSION_MIN_MONTHS = 12;
    private static final int TERM_EXTENSION_MAX_MONTHS = 240;
    private static final BigDecimal NEW_LOAN_MIN = new BigDecimal("10000000");
    private static final BigDecimal NEW_LOAN_MAX = new BigDecimal("500000000");
    private static final int CURVE_POINT_COUNT = 9;
    private static final BigDecimal DIMINISHING_RETURN_RATIO = new BigDecimal("0.3");

    // 기본 강도(레버 랭킹 막대그래프 기준값)
    public static final BigDecimal DEFAULT_MONTHLY_EXTRA_CAPACITY = new BigDecimal("500000");
    public static final BigDecimal DEFAULT_PREPAYMENT_AMOUNT = new BigDecimal("5000000");
    public static final BigDecimal DEFAULT_TERM_EXTENSION_MONTHS = BigDecimal.valueOf(120);
    public static final BigDecimal DEFAULT_NEW_LOAN_PRINCIPAL = new BigDecimal("100000000");

    private final FutureSimulationEngine engine;
    private final StressTestLoanMapper loanMapper;
    private final LoanRepaymentCalculator repaymentCalculator;

    public LeverIntensityCalculator(
            FutureSimulationEngine engine,
            StressTestLoanMapper loanMapper,
            LoanRepaymentCalculator repaymentCalculator
    ) {
        this.engine = engine;
        this.loanMapper = loanMapper;
        this.repaymentCalculator = repaymentCalculator;
    }

    public enum LeverType {
        INCOME_CHANGE,
        LOAN_PREPAYMENT,
        LOAN_TERM_EXTENSION,
        NEW_LOAN
    }

    public record IntensityPoint(BigDecimal intensity, Integer diffMonths) {
    }

    public record IntensityCurve(
            LeverType leverType,
            List<IntensityPoint> points,
            BigDecimal diminishingReturnIntensity
    ) {
    }

    // 대출이 있어야만 의미 있는 레버(조기상환/만기연장)는 대출이 하나도 없으면 화면에서 아예 제외한다.
    public boolean isLeverAvailable(Long userId, LeverType leverType) {
        if (leverType == LeverType.LOAN_PREPAYMENT || leverType == LeverType.LOAN_TERM_EXTENSION) {
            return representativeLoan(userId) != null;
        }
        return true;
    }

    // "이 대출금리가 여유자금 수익률보다 높아서/낮아서 유리해요" 같은 추천 카드 설명 문구를 만드는 데 쓴다.
    // 대출이 없으면 null — StressTestLoanRow 엔티티를 API 계층에 노출하지 않기 위해 금리 값만 뽑아준다.
    public BigDecimal representativeLoanRatePercent(Long userId) {
        StressTestLoanRow loan = representativeLoan(userId);
        return loan == null ? null : loan.getInterestRate();
    }

    // 추천 카드에 "왜/얼마나"를 숫자로 보여주기 위한 상세값. INCOME_CHANGE는 강도 자체가 이미 명확해서 null.
    public record LeverDetail(
            BigDecimal beforeMonthlyPayment,
            BigDecimal afterMonthlyPayment,
            BigDecimal extraTotalInterest
    ) {
        static final LeverDetail NONE = new LeverDetail(null, null, null);
    }

    public LeverDetail detailFor(Long userId, LeverType leverType, BigDecimal intensity) {
        return switch (leverType) {
            case INCOME_CHANGE -> LeverDetail.NONE;
            case LOAN_PREPAYMENT -> prepaymentDetail(userId, intensity);
            case LOAN_TERM_EXTENSION -> extensionDetail(userId, intensity);
            case NEW_LOAN -> newLoanDetail(intensity);
        };
    }

    private LeverDetail prepaymentDetail(Long userId, BigDecimal prepayAmount) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        if (targetLoan == null) {
            return LeverDetail.NONE;
        }
        BigDecimal actualPrepay = prepayAmount.min(targetLoan.getCurrentBalance());
        BigDecimal newBalance = targetLoan.getCurrentBalance().subtract(actualPrepay);
        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(targetLoan.getMaturityAt()));

        BigDecimal oldMonthlyPayment = monthlyPaymentOf(targetLoan.getCurrentBalance(), targetLoan, remainingMonths);
        BigDecimal newMonthlyPayment = newBalance.signum() <= 0
                ? BigDecimal.ZERO
                : monthlyPaymentOf(newBalance, targetLoan, remainingMonths);
        return new LeverDetail(oldMonthlyPayment, newMonthlyPayment, null);
    }

    private LeverDetail extensionDetail(Long userId, BigDecimal extensionMonths) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        if (targetLoan == null) {
            return LeverDetail.NONE;
        }
        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(targetLoan.getMaturityAt()));
        int newRemainingMonths = remainingMonths + extensionMonths.intValue();

        var oldResult = repaymentCalculator.calculate(
                targetLoan.getCurrentBalance(), targetLoan.getInterestRate(), remainingMonths, targetLoan.getRepaymentType());
        var newResult = repaymentCalculator.calculate(
                targetLoan.getCurrentBalance(), targetLoan.getInterestRate(), newRemainingMonths, targetLoan.getRepaymentType());

        BigDecimal extraTotalInterest = newResult.totalInterest().subtract(oldResult.totalInterest());
        return new LeverDetail(oldResult.monthlyPayment(), newResult.monthlyPayment(), extraTotalInterest);
    }

    private LeverDetail newLoanDetail(BigDecimal principal) {
        BigDecimal monthlyPayment = repaymentCalculator.calculate(
                principal, NEW_LOAN_ASSUMED_RATE_PERCENT, NEW_LOAN_ASSUMED_TERM_MONTHS, NEW_LOAN_ASSUMED_REPAYMENT_TYPE
        ).monthlyPayment();
        return new LeverDetail(BigDecimal.ZERO, monthlyPayment, null);
    }

    public Integer calculateDiffMonths(Long userId, BigDecimal goalAmount, LeverType leverType, BigDecimal intensity) {
        return calculateDiffMonths(userId, goalAmount, leverType, intensity, null);
    }

    public Integer calculateDiffMonths(Long userId, BigDecimal goalAmount, LeverType leverType, BigDecimal intensity, BigDecimal annualReturnRatePercent) {
        Integer baseline = annualReturnRatePercent == null ? engine.calculateMonthsToGoalCompound(userId, goalAmount, FutureSimulationEngine.Adjustment.NONE) : engine.calculateMonthsToGoalCompound(userId, goalAmount, FutureSimulationEngine.Adjustment.NONE, annualReturnRatePercent);
        FutureSimulationEngine.Adjustment adjustment = resolveAdjustment(userId, leverType, intensity);
        Integer adjusted = annualReturnRatePercent == null ? engine.calculateMonthsToGoalCompound(userId, goalAmount, adjustment) : engine.calculateMonthsToGoalCompound(userId, goalAmount, adjustment, annualReturnRatePercent);
        return diff(baseline, adjusted);
    }

    public IntensityCurve calculateIntensityCurve(Long userId, BigDecimal goalAmount, LeverType leverType) {
        Integer baseline = engine.calculateMonthsToGoalCompound(userId, goalAmount, FutureSimulationEngine.Adjustment.NONE);
        List<BigDecimal> intensities = intensityPointsFor(userId, leverType);

        List<IntensityPoint> points = new ArrayList<>();
        for (BigDecimal intensity : intensities) {
            FutureSimulationEngine.Adjustment adjustment = resolveAdjustment(userId, leverType, intensity);
            Integer adjusted = engine.calculateMonthsToGoalCompound(userId, goalAmount, adjustment);
            points.add(new IntensityPoint(intensity, diff(baseline, adjusted)));
        }

        return new IntensityCurve(leverType, points, findDiminishingReturnIntensity(points));
    }

    private Integer diff(Integer baseline, Integer adjusted) {
        if (baseline == null || adjusted == null) {
            return null;
        }
        return baseline - adjusted;
    }

    // ==== 레버별 강도 -> 조정값 변환 ====

    // package-private — LeverCombinationOptimizer(조합 최적화 DP)가 레버별 Adjustment를 직접 합산하는 데 재사용한다.
    FutureSimulationEngine.Adjustment resolveAdjustment(Long userId, LeverType leverType, BigDecimal intensity) {
        return switch (leverType) {
            case INCOME_CHANGE -> incomeChangeAdjustment(userId, intensity);
            case LOAN_PREPAYMENT -> loanPrepaymentAdjustment(userId, intensity);
            case LOAN_TERM_EXTENSION -> loanTermExtensionAdjustment(userId, intensity);
            case NEW_LOAN -> newLoanAdjustment(intensity);
        };
    }

    // intensity = 매달 추가로 확보할 금액(원). 소득 증가/지출 절감으로 확보한 돈을 전부 저축으로 돌린다.
    private FutureSimulationEngine.Adjustment incomeChangeAdjustment(Long userId, BigDecimal monthlyExtraCapacity) {
        return new FutureSimulationEngine.Adjustment(BigDecimal.ZERO, BigDecimal.ZERO, monthlyExtraCapacity);
    }

    // intensity = 조기상환 금액(원). 잔액이 가장 큰 대출 하나를 대표로 삼아, 그 대출을 상환한 뒤
    // 남은 만기 동안의 새 월상환액을 다시 계산해서 차액만큼 저축 여력을 늘린다.
    private FutureSimulationEngine.Adjustment loanPrepaymentAdjustment(Long userId, BigDecimal prepayAmount) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        if (targetLoan == null) {
            return FutureSimulationEngine.Adjustment.NONE;
        }
        BigDecimal actualPrepay = prepayAmount.min(targetLoan.getCurrentBalance());
        BigDecimal newBalance = targetLoan.getCurrentBalance().subtract(actualPrepay);
        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(targetLoan.getMaturityAt()));

        BigDecimal oldMonthlyPayment = monthlyPaymentOf(targetLoan.getCurrentBalance(), targetLoan, remainingMonths);
        BigDecimal newMonthlyPayment = newBalance.signum() <= 0
                ? BigDecimal.ZERO
                : monthlyPaymentOf(newBalance, targetLoan, remainingMonths);

        BigDecimal extraMonthlyCashFlow = oldMonthlyPayment.subtract(newMonthlyPayment);
        return new FutureSimulationEngine.Adjustment(actualPrepay.negate(), actualPrepay.negate(), extraMonthlyCashFlow);
    }

    // intensity = 연장 개월수. 대표 대출의 잔여만기를 그만큼 늘려서 월상환액을 다시 계산한다(총 이자는 늘지만
    // 그 부분은 이 화면 스코프 밖 — 여기선 월 현금흐름 확보 효과만 본다).
    private FutureSimulationEngine.Adjustment loanTermExtensionAdjustment(Long userId, BigDecimal extensionMonths) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        if (targetLoan == null) {
            return FutureSimulationEngine.Adjustment.NONE;
        }
        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(targetLoan.getMaturityAt()));
        int newRemainingMonths = remainingMonths + extensionMonths.intValue();

        BigDecimal oldMonthlyPayment = monthlyPaymentOf(targetLoan.getCurrentBalance(), targetLoan, remainingMonths);
        BigDecimal newMonthlyPayment = monthlyPaymentOf(targetLoan.getCurrentBalance(), targetLoan, newRemainingMonths);

        return new FutureSimulationEngine.Adjustment(BigDecimal.ZERO, BigDecimal.ZERO, oldMonthlyPayment.subtract(newMonthlyPayment));
    }

    // intensity = 신규 대출 원금. 빌린 돈은 그대로 유동자산으로 들어오지만(순자산 변화 없음), 매달 상환액이
    // 새로 생겨서 저축 여력은 줄어든다 — 가정 금리(4.5%)가 여유자금 수익률(3.08%)보다 높아, 강도가 클수록
    // 손해가 커지는 방향(단조 감소)으로 설계돼 있다.
    private FutureSimulationEngine.Adjustment newLoanAdjustment(BigDecimal principal) {
        BigDecimal monthlyPayment = repaymentCalculator.calculate(
                principal, NEW_LOAN_ASSUMED_RATE_PERCENT, NEW_LOAN_ASSUMED_TERM_MONTHS, NEW_LOAN_ASSUMED_REPAYMENT_TYPE
        ).monthlyPayment();
        return new FutureSimulationEngine.Adjustment(principal, principal, monthlyPayment.negate());
    }

    private BigDecimal monthlyPaymentOf(BigDecimal balance, StressTestLoanRow loan, int remainingMonths) {
        return repaymentCalculator.calculate(balance, loan.getInterestRate(), remainingMonths, loan.getRepaymentType())
                .monthlyPayment();
    }

    // ==== 5단계("레버 조합해보기")용 — 레버 여러 개를 한 번에 합산 ====

    public record LeverSelection(LeverType leverType, BigDecimal intensity) {
    }

    // 선택된 레버들을 하나의 Adjustment로 합친다. 조기상환·만기연장은 같은 대표 대출에 함께 작용하므로
    // (조기상환으로 줄어든 잔액을 기준으로 만기 연장의 새 월상환액을 계산해야 진짜 상호작용이 반영됨)
    // 이 둘만 예외적으로 jointLoanAdjustment()에서 함께 계산하고, 나머지 레버는 서로 독립적이라 각자
    // resolveAdjustment()로 계산한 뒤 단순 합산한다.
    public FutureSimulationEngine.Adjustment resolveCombinedAdjustment(Long userId, List<LeverSelection> selections) {
        BigDecimal prepayAmount = null;
        BigDecimal extensionMonths = null;
        FutureSimulationEngine.Adjustment combined = FutureSimulationEngine.Adjustment.NONE;

        for (LeverSelection selection : selections) {
            if (selection.leverType() == LeverType.LOAN_PREPAYMENT) {
                prepayAmount = selection.intensity();
            } else if (selection.leverType() == LeverType.LOAN_TERM_EXTENSION) {
                extensionMonths = selection.intensity();
            } else {
                combined = combine(combined, resolveAdjustment(userId, selection.leverType(), selection.intensity()));
            }
        }

        if (prepayAmount != null || extensionMonths != null) {
            combined = combine(combined, jointLoanAdjustment(userId, prepayAmount, extensionMonths));
        }
        return combined;
    }

    // 조기상환(잔액 감소)과 만기연장(상환기간 증가)을 같은 대표 대출에 함께 적용한 새 월상환액을 계산한다.
    // 둘 중 하나만 선택됐으면 나머지는 0으로 들어와 각각의 단일 레버 계산과 동일하게 자연스럽게 축소된다.
    private FutureSimulationEngine.Adjustment jointLoanAdjustment(Long userId, BigDecimal prepayAmount, BigDecimal extensionMonths) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        if (targetLoan == null) {
            return FutureSimulationEngine.Adjustment.NONE;
        }
        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(targetLoan.getMaturityAt()));
        BigDecimal oldMonthlyPayment = monthlyPaymentOf(targetLoan.getCurrentBalance(), targetLoan, remainingMonths);

        BigDecimal actualPrepay = prepayAmount != null
                ? prepayAmount.min(targetLoan.getCurrentBalance())
                : BigDecimal.ZERO;
        BigDecimal newBalance = targetLoan.getCurrentBalance().subtract(actualPrepay);
        int newRemainingMonths = remainingMonths + (extensionMonths != null ? extensionMonths.intValue() : 0);

        BigDecimal newMonthlyPayment = newBalance.signum() <= 0
                ? BigDecimal.ZERO
                : monthlyPaymentOf(newBalance, targetLoan, newRemainingMonths);

        BigDecimal extraMonthlyCashFlow = oldMonthlyPayment.subtract(newMonthlyPayment);
        return new FutureSimulationEngine.Adjustment(actualPrepay.negate(), actualPrepay.negate(), extraMonthlyCashFlow);
    }

    private FutureSimulationEngine.Adjustment combine(FutureSimulationEngine.Adjustment a, FutureSimulationEngine.Adjustment b) {
        return new FutureSimulationEngine.Adjustment(
                a.liquidAssetsDelta().add(b.liquidAssetsDelta()),
                a.totalDebtDelta().add(b.totalDebtDelta()),
                a.monthlyCashFlowDelta().add(b.monthlyCashFlowDelta())
        );
    }

    // ==== 강도 포인트 생성 ====

    public List<BigDecimal> intensityPointsFor(Long userId, LeverType leverType) {
        return switch (leverType) {
            case INCOME_CHANGE -> incomeChangePoints();
            case LOAN_PREPAYMENT -> evenlySpaced(PREPAYMENT_MIN, prepaymentMax(userId), CURVE_POINT_COUNT);
            case LOAN_TERM_EXTENSION -> evenlySpaced(
                    BigDecimal.valueOf(TERM_EXTENSION_MIN_MONTHS), BigDecimal.valueOf(TERM_EXTENSION_MAX_MONTHS), CURVE_POINT_COUNT
            );
            case NEW_LOAN -> evenlySpaced(NEW_LOAN_MIN, NEW_LOAN_MAX, CURVE_POINT_COUNT);
        };
    }

    private List<BigDecimal> incomeChangePoints() {
        return List.of(new BigDecimal("100000"), new BigDecimal("500000"), new BigDecimal("1000000"));
    }

    private BigDecimal prepaymentMax(Long userId) {
        StressTestLoanRow targetLoan = representativeLoan(userId);
        BigDecimal balance = targetLoan != null ? targetLoan.getCurrentBalance() : BigDecimal.ZERO;
        return PREPAYMENT_MAX_CAP.min(balance).max(PREPAYMENT_MIN);
    }

    private List<BigDecimal> evenlySpaced(BigDecimal min, BigDecimal max, int count) {
        List<BigDecimal> points = new ArrayList<>();
        if (max.compareTo(min) <= 0) {
            points.add(min);
            return points;
        }
        BigDecimal step = max.subtract(min).divide(BigDecimal.valueOf(count - 1), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < count; i++) {
            points.add(i == count - 1 ? max : min.add(step.multiply(BigDecimal.valueOf(i))));
        }
        return points;
    }

    // ==== 대출 조회 ====

    // package-private — RateRiskReferenceService(섹션 2)도 같은 "대표 대출" 기준으로 재사용한다.
    StressTestLoanRow representativeLoan(Long userId) {
        return loanMapper.findNormalLoansByUserId(userId).stream()
                .max(Comparator.comparing(StressTestLoanRow::getCurrentBalance))
                .orElse(null);
    }

    // ==== 효과 둔화 지점 판단 ====
    // 증분(diffMonths 변화량)이 "바로 이전 구간" 대비 30% 이하로 떨어지는 첫 지점의 intensity를 반환한다.
    // 데이터가 부족하거나(포인트 3개 미만) 계속 증분이 유지되면 null.
    // package-private — 순수 알고리즘이라 실제 엔진 계산 없이 합성 데이터로 직접 단위 테스트한다.
    BigDecimal findDiminishingReturnIntensity(List<IntensityPoint> points) {
        List<BigDecimal> increments = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            Integer prev = points.get(i - 1).diffMonths();
            Integer curr = points.get(i).diffMonths();
            increments.add(prev == null || curr == null ? null : BigDecimal.valueOf(Math.abs(curr - prev)));
        }

        for (int i = 1; i < increments.size(); i++) {
            BigDecimal prevInc = increments.get(i - 1);
            BigDecimal currInc = increments.get(i);
            if (prevInc == null || currInc == null || prevInc.signum() <= 0) {
                continue;
            }
            BigDecimal ratio = currInc.divide(prevInc, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(DIMINISHING_RETURN_RATIO) <= 0) {
                // increments[i]는 points[i] -> points[i+1] 구간이므로, 둔화가 시작되는 지점은 points[i].
                return points.get(i).intensity();
            }
        }
        return null;
    }

    public BigDecimal defaultIntensityFor(LeverType leverType) {
        return switch (leverType) {
            case INCOME_CHANGE -> DEFAULT_MONTHLY_EXTRA_CAPACITY;
            case LOAN_PREPAYMENT -> DEFAULT_PREPAYMENT_AMOUNT;
            case LOAN_TERM_EXTENSION -> DEFAULT_TERM_EXTENSION_MONTHS;
            case NEW_LOAN -> DEFAULT_NEW_LOAN_PRINCIPAL;
        };
    }

    // 추천 카드의 강도 칩(2~3개)에 쓰는 값 — 최소/기본/최대 강도를 그대로 쓴다. 슬라이더 대신 몇 개의
    // 고정된 값 중에서 고르게 하는 방식으로 바꾸면서, 레버당 딱 이 값들만 필요해졌다.
    public List<BigDecimal> presetIntensitiesFor(Long userId, LeverType leverType) {
        List<BigDecimal> points = intensityPointsFor(userId, leverType);
        BigDecimal min = points.get(0);
        BigDecimal max = points.get(points.size() - 1);
        BigDecimal defaultValue = defaultIntensityFor(leverType).min(max).max(min);

        List<BigDecimal> presets = new ArrayList<>();
        presets.add(min);
        if (defaultValue.compareTo(min) != 0 && defaultValue.compareTo(max) != 0) {
            presets.add(defaultValue);
        }
        if (max.compareTo(min) != 0) {
            presets.add(max);
        }
        return presets;
    }
}
