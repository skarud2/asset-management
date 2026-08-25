package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.loan.ratesimulation.common.dto.response.RepaymentCalculationResult;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleProjectionService {

    // 기본 물가상승률 (연 2.0%)
    public static final BigDecimal DEFAULT_INFLATION_RATE = new BigDecimal("0.02");

    private final LoanRepaymentCalculator loanRepaymentCalculator;

    /**
     * 1. 시뮬레이션 초기 금융상태(T0) 생성
     */
    public LifecycleFinancialStateDto createInitialState(LifecycleBaseStateDto baseState) {
        if (baseState == null) {
            return LifecycleFinancialStateDto.builder()
                    .stateDate(LocalDate.now())
                    .build();
        }

        LocalDate baseDate = baseState.getBaseDate() != null ? baseState.getBaseDate() : LocalDate.now();

        // 기존 LoanRepaymentCalculator를 활용하여 총 부채 및 월 상환액 합산
        BigDecimal totalDebt = calculateTotalDebt(baseState.getLoans());
        BigDecimal monthlyDebtPayment = calculateTotalMonthlyPayment(baseState.getLoans(), baseDate);

        java.util.List<LifecycleLoanDto> initialLoans = new java.util.ArrayList<>();
        if (baseState.getLoans() != null) {
            initialLoans.addAll(baseState.getLoans());
        }

        LifecycleFinancialStateDto initialState = LifecycleFinancialStateDto.builder()
                .stateDate(baseDate)
                .cashAsset(nvl(baseState.getLiquidAssetAmount()))
                .realEstateAsset(BigDecimal.ZERO)
                .depositAsset(BigDecimal.ZERO)
                .housingAsset(BigDecimal.ZERO)
                .currentHousingType(baseState.getCurrentHousingType() != null ? baseState.getCurrentHousingType() : "FAMILY")
                .totalDebt(totalDebt)
                .loans(initialLoans)
                .annualIncome(nvl(baseState.getAnnualIncome()))
                .monthlySupportIncome(BigDecimal.ZERO)
                .monthlyLivingExpense(nvl(baseState.getMonthlyLivingExpense()))
                .monthlyHousingExpense(nvl(baseState.getMonthlyHousingExpense()))
                .monthlyDebtPayment(monthlyDebtPayment)
                .build();

        // 초기 저축여력 및 DSR 계산
        initialState.recalculateMonthlySavingCapacity();
        initialState.setDsr(calculateDsr(monthlyDebtPayment, initialState.getAnnualIncome()));

        log.info("[LifecycleProjectionService] Initial state created for userId: {}, cashAsset: {}, monthlySavingCapacity: {}, DSR: {}%",
                baseState.getUserId(), initialState.getCashAsset(), initialState.getMonthlySavingCapacity(), initialState.getDsr());

        return initialState;
    }

    /**
     * 2. 미래 특정 시점으로 시간 전진 (복리 소득/물가 상승, 대출 상환/만기 소멸 반영, 매월 저축액 누적 및 동적 DSR 산출)
     *
     * @param currentState          현재 시점의 금융 상태
     * @param targetDate            목표 시점 (다음 이벤트 날짜)
     * @param annualSalaryGrowthRate 연간 급여 상승률 (예: 0.031 = 3.1%)
     * @param annualInflationRate   연간 물가 상승률 (예: 0.02 = 2.0%, null일 경우 기본 2%)
     * @param loans                 사용자 보유 대출 목록 (만기 체크 및 잔여 원금 재계산용)
     * @return 목표 시점의 갱신된 금융 상태
     */
    public LifecycleFinancialStateDto project(
            LifecycleFinancialStateDto currentState,
            LocalDate targetDate,
            BigDecimal annualSalaryGrowthRate,
            BigDecimal annualInflationRate,
            List<LifecycleLoanDto> loans
    ) {
        if (currentState == null || targetDate == null) {
            return currentState;
        }

        List<LifecycleLoanDto> effectiveLoans = loans != null ? loans : currentState.getLoans();

        LocalDate startDate = currentState.getStateDate();
        if (startDate == null || !targetDate.isAfter(startDate)) {
            return currentState.copy(targetDate);
        }

        int totalMonths = (int) ChronoUnit.MONTHS.between(startDate, targetDate);
        if (totalMonths <= 0) {
            return currentState.copy(targetDate);
        }

        double years = totalMonths / 12.0;
        double salaryRate = annualSalaryGrowthRate != null ? annualSalaryGrowthRate.doubleValue() : 0.031;
        double inflationRate = annualInflationRate != null ? annualInflationRate.doubleValue() : DEFAULT_INFLATION_RATE.doubleValue();

        // 1) 미래 연소득 복리 상승: Income * (1 + salaryRate)^years
        double incomeCompoundFactor = Math.pow(1.0 + salaryRate, years);
        BigDecimal projectedAnnualIncome = currentState.getAnnualIncome()
                .multiply(BigDecimal.valueOf(incomeCompoundFactor))
                .setScale(0, RoundingMode.HALF_UP);

        // 2) 미래 월생활비 물가상승 복리 반영: Expense * (1 + inflationRate)^years
        double inflationCompoundFactor = Math.pow(1.0 + inflationRate, years);
        BigDecimal projectedLivingExpense = currentState.getMonthlyLivingExpense()
                .multiply(BigDecimal.valueOf(inflationCompoundFactor))
                .setScale(0, RoundingMode.HALF_UP);

        // 3) 미래 목표 시점(targetDate)의 남은 대출 잔액 및 월 상환액 재계산 (만기 도래 대출은 0원으로 소멸)
        BigDecimal projectedTotalDebt = effectiveLoans != null
                ? calculateProjectedTotalDebt(effectiveLoans, targetDate)
                : nvl(currentState.getTotalDebt());
        BigDecimal projectedMonthlyDebtPayment = effectiveLoans != null
                ? calculateProjectedMonthlyPayment(effectiveLoans, targetDate)
                : nvl(currentState.getMonthlyDebtPayment());

        // 4) 기간 동안 누적 저축액 계산 (시작점과 끝점의 평균 월 저축여력 * 경과 개월수)
        BigDecimal startMonthlyIncome = currentState.getAnnualIncome().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal endMonthlyIncome = projectedAnnualIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal startMonthlySaving = startMonthlyIncome
                .add(currentState.getMonthlySupportIncome())
                .subtract(currentState.getMonthlyLivingExpense())
                .subtract(currentState.getMonthlyHousingExpense())
                .subtract(currentState.getMonthlyDebtPayment());

        BigDecimal endMonthlySaving = endMonthlyIncome
                .add(currentState.getMonthlySupportIncome())
                .subtract(projectedLivingExpense)
                .subtract(currentState.getMonthlyHousingExpense())
                .subtract(projectedMonthlyDebtPayment);

        BigDecimal avgMonthlySaving = startMonthlySaving.add(endMonthlySaving)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        BigDecimal accumulatedSavings = avgMonthlySaving.multiply(BigDecimal.valueOf(totalMonths));
        BigDecimal projectedCashAsset = currentState.getCashAsset().add(accumulatedSavings);

        // 5) 미래 DSR 재계산 (상환액 감소 + 소득 증가로 낮아진 DSR 반영)
        BigDecimal projectedDsr = calculateDsr(projectedMonthlyDebtPayment, projectedAnnualIncome);

        LifecycleFinancialStateDto projectedState = currentState.toBuilder()
                .stateDate(targetDate)
                .cashAsset(projectedCashAsset)
                .totalDebt(projectedTotalDebt)
                .loans(effectiveLoans)
                .annualIncome(projectedAnnualIncome)
                .monthlyLivingExpense(projectedLivingExpense)
                .monthlyDebtPayment(projectedMonthlyDebtPayment)
                .dsr(projectedDsr)
                .build();

        projectedState.recalculateMonthlySavingCapacity();

        log.info("[LifecycleProjectionService] Projected from {} to {} ({} months). Cash: {} -> {}, DebtPayment: {} -> {}, DSR: {}% -> {}%",
                startDate, targetDate, totalMonths, currentState.getCashAsset(), projectedCashAsset,
                currentState.getMonthlyDebtPayment(), projectedMonthlyDebtPayment, currentState.getDsr(), projectedDsr);

        return projectedState;
    }

    /**
     * 기존 메서드 호환용 오버로딩 (loans가 없는 경우)
     */
    public LifecycleFinancialStateDto project(
            LifecycleFinancialStateDto currentState,
            LocalDate targetDate,
            BigDecimal annualSalaryGrowthRate,
            BigDecimal annualInflationRate
    ) {
        return project(currentState, targetDate, annualSalaryGrowthRate, annualInflationRate, null);
    }

    /**
     * 3. 연도별 타임라인 스냅샷 생성 (차트용: 1년 단위 연속 데이터)
     */
    public List<LifecycleFinancialStateDto> generateAnnualTimeline(
            LifecycleFinancialStateDto startState,
            LocalDate endDate,
            BigDecimal annualSalaryGrowthRate,
            BigDecimal annualInflationRate,
            List<LifecycleLoanDto> loans
    ) {
        List<LifecycleFinancialStateDto> timeline = new ArrayList<>();
        if (startState == null || endDate == null) {
            return timeline;
        }

        timeline.add(startState);
        LifecycleFinancialStateDto current = startState;
        LocalDate nextYearDate = startState.getStateDate().plusYears(1);

        while (!nextYearDate.isAfter(endDate)) {
            current = project(current, nextYearDate, annualSalaryGrowthRate, annualInflationRate, loans);
            timeline.add(current);
            nextYearDate = nextYearDate.plusYears(1);
        }

        if (current.getStateDate().isBefore(endDate)) {
            timeline.add(project(current, endDate, annualSalaryGrowthRate, annualInflationRate, loans));
        }

        return timeline;
    }

    /**
     * 4. 일회성 비용 미래가치 환산 유틸
     * (예: 2026년 기준 4,500만원 결혼비용 -> 2031년 물가 환산비용 계산)
     */
    public BigDecimal adjustCostForInflation(
            BigDecimal baseCost,
            LocalDate baseDate,
            LocalDate targetDate,
            BigDecimal annualInflationRate
    ) {
        if (baseCost == null || baseCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (baseDate == null || targetDate == null || !targetDate.isAfter(baseDate)) {
            return baseCost;
        }

        int months = (int) ChronoUnit.MONTHS.between(baseDate, targetDate);
        double years = months / 12.0;
        double inflation = annualInflationRate != null ? annualInflationRate.doubleValue() : DEFAULT_INFLATION_RATE.doubleValue();

        double factor = Math.pow(1.0 + inflation, years);
        return baseCost.multiply(BigDecimal.valueOf(factor)).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 특정 시점(targetDate)의 총 부채 잔액 계산 (만기 지난 대출은 0원, 분할상환 대출은 잔여원금 계산)
     */
    public BigDecimal calculateProjectedTotalDebt(List<LifecycleLoanDto> loans, LocalDate targetDate) {
        if (loans == null || loans.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate standardDate = targetDate != null ? targetDate : LocalDate.now();
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (LifecycleLoanDto loan : loans) {
            if (loan.getCurrentBalance() == null || loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 만기일이 지났으면 부채 잔액 0원 (완납)
            if (loan.getMaturityAt() != null && !loan.getMaturityAt().isAfter(standardDate)) {
                continue;
            }

            // 만기일이 남아있는 경우 잔여원금 계산
            int remainingMonths = 60;
            if (loan.getMaturityAt() != null) {
                remainingMonths = loanRepaymentCalculator.calculateRemainingMonths(standardDate, loan.getMaturityAt());
                if (remainingMonths <= 0) remainingMonths = 1;
            }

            BigDecimal ratePercent = toRatePercent(loan.getInterestRate());
            String repaymentType = loan.getRepaymentType() != null ? loan.getRepaymentType() : "만기일시상환";

            // 만기일시상환은 만기 전까지 원금이 그대로 유지됨
            if (repaymentType.contains("만기일시") || repaymentType.contains("BULLET")) {
                totalDebt = totalDebt.add(loan.getCurrentBalance());
            } else {
                // 원리금/원금 분할상환 대출: 경과기간에 따른 잔여원금 계산
                try {
                    int totalTermMonths = remainingMonths + (int) ChronoUnit.MONTHS.between(LocalDate.now(), standardDate);
                    int monthsElapsed = (int) ChronoUnit.MONTHS.between(LocalDate.now(), standardDate);
                    if (totalTermMonths <= 0) totalTermMonths = 60;
                    if (monthsElapsed < 0) monthsElapsed = 0;
                    if (monthsElapsed > totalTermMonths) monthsElapsed = totalTermMonths;

                    BigDecimal remainingBalance = loanRepaymentCalculator.calculateRemainingBalance(
                            loan.getCurrentBalance(),
                            ratePercent,
                            totalTermMonths,
                            monthsElapsed,
                            repaymentType
                    );
                    totalDebt = totalDebt.add(remainingBalance != null ? remainingBalance : loan.getCurrentBalance());
                } catch (Exception e) {
                    totalDebt = totalDebt.add(loan.getCurrentBalance());
                }
            }
        }

        return totalDebt;
    }

    /**
     * 특정 시점(targetDate)의 월 대출 상환액 계산 (만기 지난 대출은 0원)
     */
    public BigDecimal calculateProjectedMonthlyPayment(List<LifecycleLoanDto> loans, LocalDate targetDate) {
        return calculateTotalMonthlyPayment(loans, targetDate);
    }

    /**
     * 총 대출잔액 합산
     */
    public BigDecimal calculateTotalDebt(List<LifecycleLoanDto> loans) {
        if (loans == null || loans.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return loans.stream()
                .map(LifecycleLoanDto::getCurrentBalance)
                .filter(b -> b != null && b.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 특정 기준일(baseDate) 시점의 월 총 대출상환액 계산 (만기 지난 대출은 자동 제외)
     */
    public BigDecimal calculateTotalMonthlyPayment(List<LifecycleLoanDto> loans, LocalDate baseDate) {
        if (loans == null || loans.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate standardDate = baseDate != null ? baseDate : LocalDate.now();
        BigDecimal totalPayment = BigDecimal.ZERO;

        for (LifecycleLoanDto loan : loans) {
            if (loan.getCurrentBalance() == null || loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 만기일이 지났으면 상환 제외 (0원)
            if (loan.getMaturityAt() != null && !loan.getMaturityAt().isAfter(standardDate)) {
                continue;
            }

            int remainingMonths = 60; // 기본 5년
            if (loan.getMaturityAt() != null) {
                remainingMonths = loanRepaymentCalculator.calculateRemainingMonths(standardDate, loan.getMaturityAt());
                if (remainingMonths <= 0) remainingMonths = 1;
            }

            BigDecimal ratePercent = toRatePercent(loan.getInterestRate());
            String repaymentType = loan.getRepaymentType() != null ? loan.getRepaymentType() : "만기일시상환";

            try {
                RepaymentCalculationResult result = loanRepaymentCalculator.calculate(
                        loan.getCurrentBalance(),
                        ratePercent,
                        remainingMonths,
                        repaymentType
                );
                if (result != null && result.monthlyPayment() != null) {
                    totalPayment = totalPayment.add(result.monthlyPayment());
                }
            } catch (Exception e) {
                log.warn("[LifecycleProjectionService] Failed to calculate payment for loan: {}, fallback to interest-only", loan.getLoanAccountId(), e);
                // 실패 시 간이 이자만 계산: balance * (rate / 12)
                BigDecimal monthlyInterest = loan.getCurrentBalance()
                        .multiply(ratePercent.divide(BigDecimal.valueOf(1200), 6, RoundingMode.HALF_UP))
                        .setScale(0, RoundingMode.HALF_UP);
                totalPayment = totalPayment.add(monthlyInterest);
            }
        }

        return totalPayment;
    }

    /**
     * DSR 계산: (연간 원리금상환액 / 연소득) * 100 (%)
     */
    public BigDecimal calculateDsr(BigDecimal monthlyDebtPayment, BigDecimal annualIncome) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0 || monthlyDebtPayment == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal annualPayment = monthlyDebtPayment.multiply(BigDecimal.valueOf(12));
        return annualPayment.multiply(BigDecimal.valueOf(100))
                .divide(annualIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * 0.035 -> 3.5% 또는 3.5 -> 3.5%로 정규화
     */
    private BigDecimal toRatePercent(BigDecimal rate) {
        if (rate == null) return new BigDecimal("4.0"); // 기본 4.0%
        if (rate.compareTo(BigDecimal.ONE) < 0 && rate.compareTo(BigDecimal.ZERO) > 0) {
            return rate.multiply(BigDecimal.valueOf(100));
        }
        return rate;
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
