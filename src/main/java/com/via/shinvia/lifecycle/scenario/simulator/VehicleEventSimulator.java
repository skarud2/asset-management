package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [3. 차량 구매 생애주기 시뮬레이터]
 * - 차량가, 취등록세(7%), 오토론 대출, 월 유지비 매핑
 * - 값 부재 시 0 또는 빈 문자열을 기본으로 안전하게 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.VEHICLE_PURCHASE;
    }

    @Override
    public LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal totalCost = resolveVehiclePrice(input);
        BigDecimal requiredAmount = input.getUserRequiredAmount() != null
                ? input.getUserRequiredAmount()
                : totalCost;

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 자기자금(선납금/취등록세) 현금 차감
        if (beforeCash.compareTo(requiredAmount) >= 0) {
            afterCash = beforeCash.subtract(requiredAmount);
            summary = String.format("차량 구매 자기자금으로 약 %s원이 지출되었습니다.", formatMoney(requiredAmount));
        } else {
            fundingShortage = requiredAmount.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("차량 구매 자기자금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 2. 월 생활비 증가 (유지비)
        BigDecimal additionalExpense = input.getAdditionalMonthlyExpense() != null
                ? input.getAdditionalMonthlyExpense()
                : resolveMonthlyMaintenanceCost(input);
        BigDecimal newLivingExpense = nvl(beforeState.getMonthlyLivingExpense()).add(nvl(additionalExpense));

        // 3. 정식 금융 대출(오토론 등) 발생 시 부채 및 DSR 반영
        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal newTotalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal newDebtPayment = nvl(beforeState.getMonthlyDebtPayment());

        List<LifecycleLoanDto> updatedLoans = new ArrayList<>();
        if (beforeState.getLoans() != null) {
            for (var l : beforeState.getLoans()) {
                if (l != null) updatedLoans.add(l);
            }
        }

        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            newTotalDebt = newTotalDebt.add(newLoanAmount);
            int periodMonths = input.getLoanPeriodMonths() != null ? input.getLoanPeriodMonths() : 60;
            BigDecimal rate = input.getLoanInterestRate() != null ? input.getLoanInterestRate() : new BigDecimal("5.0");

            // 연 5.0% 원리금균등 분할상환 계산
            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        newLoanAmount,
                        rate,
                        periodMonths,
                        "원리금균등상환"
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    newDebtPayment = newDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                BigDecimal monthlyPrincipal = newLoanAmount.divide(BigDecimal.valueOf(periodMonths), 0, RoundingMode.HALF_UP);
                BigDecimal monthlyInterest = newLoanAmount.multiply(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                newDebtPayment = newDebtPayment.add(monthlyPrincipal).add(monthlyInterest);
            }

            LocalDate eventDate = input.getTargetDate() != null ? input.getTargetDate() : LocalDate.now();
            updatedLoans.add(LifecycleLoanDto.builder()
                    .loanAccountId(System.currentTimeMillis())
                    .loanType("AUTO")
                    .currentBalance(newLoanAmount)
                    .interestRate(rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .rateType("FIXED")
                    .repaymentType("원리금균등상환")
                    .maturityAt(eventDate.plusMonths(periodMonths))
                    .build());
        }

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .totalDebt(newTotalDebt)
                .loans(updatedLoans)
                .monthlyLivingExpense(newLivingExpense)
                .monthlyDebtPayment(newDebtPayment)
                .build();

        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[VehicleEventSimulator] Simulated. Required: {}, NewLoan: {}, AdditionalExpense: {}, NewDSR: {}%",
                requiredAmount, newLoanAmount, additionalExpense, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.VEHICLE_PURCHASE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary != null ? summary : "")
                .build();
    }

    private BigDecimal resolveVehiclePrice(LifecycleEventInput input) {
        if (input.getEstimatedCost() != null && input.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
            return input.getEstimatedCost();
        }

        try {
            return referenceService.getNationalAmount(
                    LifecycleEventType.VEHICLE_PURCHASE,
                    "VEHICLE_BASE_PRICE",
                    null
            );
        } catch (Exception e) {
            return new BigDecimal("36000000.00");
        }
    }

    private BigDecimal resolveMonthlyMaintenanceCost(LifecycleEventInput input) {
        try {
            return referenceService.getVehicleAmount(
                    "VEHICLE_MONTHLY_MAINTENANCE_COST",
                    VehicleClass.MIDSIZE,
                    null
            );
        } catch (Exception e) {
            return new BigDecimal("400000.00");
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
