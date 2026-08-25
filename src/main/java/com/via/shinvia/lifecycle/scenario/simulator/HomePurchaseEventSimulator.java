package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
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
 * [6. 내 집 마련(주택 구매) 생애주기 시뮬레이터]
 * - 기존 임차보증금 회수, 주택매매가, 취득세, 주택담보대출 매핑
 * - 관리비·금리·세율은 생애주기 DB 기준값을 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HomePurchaseEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.HOME_PURCHASE;
    }

    @Override
    public LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal totalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal monthlyDebtPayment = nvl(beforeState.getMonthlyDebtPayment());
        List<LifecycleLoanDto> updatedLoans = new ArrayList<>();
        if (beforeState.getLoans() != null) {
            for (var l : beforeState.getLoans()) {
                if (l != null) updatedLoans.add(l);
            }
        }

        // 1. 기존 임차 보증금 회수 (주택 매매로 전월세 보증금이 현금으로 전환)
        BigDecimal existingDeposit = nvl(beforeState.getDepositAsset());
        BigDecimal currentCash = beforeCash.add(existingDeposit);

        // 2. 주택 매매가 및 취득세
        BigDecimal purchasePrice = input.getAcquiredAssetAmount() != null && input.getAcquiredAssetAmount().compareTo(BigDecimal.ZERO) > 0
                ? input.getAcquiredAssetAmount()
                : resolveBasePurchasePrice(input);

        BigDecimal taxRate = resolveAcquisitionTaxRate(input);
        BigDecimal acquisitionTax = purchasePrice.multiply(taxRate);
        BigDecimal totalCost = purchasePrice.add(acquisitionTax);

        // 3. 신규 주택담보대출 및 자기자본 요구액 계산
        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal cashInflow = nvl(input.getCashInflowAmount());
        BigDecimal requiredCash = input.getUserRequiredAmount() != null
                ? input.getUserRequiredAmount()
                : totalCost.subtract(newLoanAmount).subtract(cashInflow).max(BigDecimal.ZERO);

        BigDecimal afterCash = currentCash.subtract(requiredCash);
        BigDecimal fundingShortage = afterCash.signum() < 0 ? afterCash.abs() : BigDecimal.ZERO;
        String summary;

        if (fundingShortage.signum() == 0) {
            summary = String.format("내 집 마련 자기자본으로 약 %s원이 지출되었습니다.", formatMoney(requiredCash));
        } else {
            summary = String.format("내 집 마련 자기자본 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 주택담보대출 실행 및 원리금 균등상환액 계산
        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalDebt = totalDebt.add(newLoanAmount);
            int periodMonths = input.getLoanPeriodMonths() != null ? input.getLoanPeriodMonths() : 360;
            BigDecimal interestRate = input.getLoanInterestRate() != null
                    ? input.getLoanInterestRate()
                    : referenceService.getNationalRate(LifecycleEventType.HOME_PURCHASE, "HOME_LOAN_INTEREST_RATE", null);
            String repaymentType = repaymentTypeLabel(input.getLoanRepaymentType());

            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        newLoanAmount,
                        interestRate,
                        periodMonths,
                        repaymentType
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    monthlyDebtPayment = monthlyDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                BigDecimal monthlyPrincipal = newLoanAmount.divide(BigDecimal.valueOf(periodMonths), 0, RoundingMode.HALF_UP);
                BigDecimal monthlyInterest = newLoanAmount.multiply(interestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                monthlyDebtPayment = monthlyDebtPayment.add(monthlyPrincipal).add(monthlyInterest);
            }

            LocalDate eventDate = input.getTargetDate() != null ? input.getTargetDate() : LocalDate.now();
            updatedLoans.add(LifecycleLoanDto.builder()
                    .loanAccountId(System.currentTimeMillis())
                    .loanType("MORTGAGE")
                    .currentBalance(newLoanAmount)
                    .interestRate(interestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .rateType("FIXED")
                    .repaymentType(repaymentType)
                    .maturityAt(eventDate.plusMonths(periodMonths))
                    .build());
        }

        // 5. 월 주거비 (자가는 월세 0원, 아파트 관리비 약 20만원 가정)
        BigDecimal newHousingExpense = input.getAdditionalMonthlyExpense() != null
                ? input.getAdditionalMonthlyExpense()
                : referenceService.getNationalAmount(LifecycleEventType.HOME_PURCHASE, "HOME_MONTHLY_MAINTENANCE_COST", null);

        // 6. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .depositAsset(BigDecimal.ZERO)
                .realEstateAsset(purchasePrice)
                .currentHousingType("OWN")
                .monthlyHousingExpense(newHousingExpense)
                .totalDebt(totalDebt)
                .monthlyDebtPayment(monthlyDebtPayment)
                .loans(updatedLoans)
                .build();

        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = monthlyDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[HomePurchaseEventSimulator] Simulated. PurchasePrice: {}, Loan: {}, RequiredCash: {}, DSR: {}%",
                purchasePrice, newLoanAmount, requiredCash, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.HOME_PURCHASE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(cashInflow)
                .fundingShortage(fundingShortage)
                .summary(summary != null ? summary : "")
                .build();
    }

    private String repaymentTypeLabel(String repaymentType) {
        if ("EQUAL_PRINCIPAL".equals(repaymentType)) return "원금균등상환";
        if ("BULLET".equals(repaymentType)) return "만기일시상환";
        return "원리금균등상환";
    }

    private BigDecimal resolveBasePurchasePrice(LifecycleEventInput input) {
        return referenceService.getNationalAmount(
                LifecycleEventType.HOME_PURCHASE,
                "HOME_BASE_PURCHASE_PRICE",
                null
        );
    }

    private BigDecimal resolveAcquisitionTaxRate(LifecycleEventInput input) {
        return referenceService.getNationalRate(
                LifecycleEventType.HOME_PURCHASE,
                "ACQUISITION_TAX_RATE",
                null
        );
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
