package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
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
 * [5. 전세 주거 생애주기 시뮬레이터]
 * - 기존 보증금 회수, 신규 전세보증금, 전세자금대출(만기일시상환 이자) 매핑
 * - 값 부재 시 0 또는 빈 문자열을 기본으로 안전하게 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JeonseEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.JEONSE;
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

        // 1. 기존 임차 보증금 회수
        BigDecimal existingDeposit = nvl(beforeState.getDepositAsset());
        BigDecimal currentCash = beforeCash.add(existingDeposit);

        // 2. 기존 주택 매각 선택 시
        BigDecimal existingRealEstate = nvl(beforeState.getRealEstateAsset());
        BigDecimal newRealEstate = existingRealEstate;
        if (Boolean.FALSE.equals(input.getKeepExistingHome()) && existingRealEstate.compareTo(BigDecimal.ZERO) > 0) {
            currentCash = currentCash.add(existingRealEstate);
            newRealEstate = BigDecimal.ZERO;

            BigDecimal mortgageBalance = BigDecimal.ZERO;
            List<LifecycleLoanDto> remainingLoans = new ArrayList<>();
            for (LifecycleLoanDto loan : updatedLoans) {
                if ("MORTGAGE".equalsIgnoreCase(loan.getLoanType())) {
                    mortgageBalance = mortgageBalance.add(nvl(loan.getCurrentBalance()));
                } else {
                    remainingLoans.add(loan);
                }
            }
            if (mortgageBalance.compareTo(BigDecimal.ZERO) > 0) {
                currentCash = currentCash.subtract(mortgageBalance);
                totalDebt = totalDebt.subtract(mortgageBalance).max(BigDecimal.ZERO);
                updatedLoans = remainingLoans;
            }
        }

        // 3. 신규 전세 보증금 및 전세대출
        BigDecimal newDeposit = input.getAcquiredAssetAmount() != null && input.getAcquiredAssetAmount().compareTo(BigDecimal.ZERO) > 0
                ? input.getAcquiredAssetAmount()
                : resolveBaseDeposit(input);

        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal cashInflow = nvl(input.getCashInflowAmount());
        BigDecimal requiredCash = newDeposit.subtract(newLoanAmount).subtract(cashInflow).max(BigDecimal.ZERO);

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        if (currentCash.compareTo(requiredCash) >= 0) {
            afterCash = currentCash.subtract(requiredCash);
            summary = String.format("전세 보증금 자기자금으로 약 %s원이 지출되었습니다.", formatMoney(requiredCash));
        } else {
            fundingShortage = requiredCash.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("전세 보증금 자기자금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 전세대출 등록 (만기일시상환 - 월 이자만 납부)
        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalDebt = totalDebt.add(newLoanAmount);
            BigDecimal interestRate = input.getLoanInterestRate() != null ? input.getLoanInterestRate() : new BigDecimal("3.8");
            BigDecimal monthlyInterest = newLoanAmount.multiply(interestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                    .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
            monthlyDebtPayment = monthlyDebtPayment.add(monthlyInterest);

            LocalDate eventDate = input.getTargetDate() != null ? input.getTargetDate() : LocalDate.now();
            int loanPeriodMonths = input.getLoanPeriodMonths() != null ? input.getLoanPeriodMonths() : 24;

            updatedLoans.add(LifecycleLoanDto.builder()
                    .loanAccountId(System.currentTimeMillis())
                    .loanType("JEONSE")
                    .currentBalance(newLoanAmount)
                    .interestRate(interestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .rateType("FIXED")
                    .repaymentType("만기일시상환")
                    .maturityAt(eventDate.plusMonths(loanPeriodMonths))
                    .build());
        }

        // 5. 월 주거비용 (전세는 월세 0원, 기본 관리비만)
        BigDecimal newHousingExpense = input.getAdditionalMonthlyExpense() != null
                ? input.getAdditionalMonthlyExpense()
                : new BigDecimal("100000.00");

        // 6. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .depositAsset(newDeposit)
                .realEstateAsset(newRealEstate)
                .currentHousingType("JEONSE")
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

        log.info("[JeonseEventSimulator] Simulated. NewDeposit: {}, NewLoan: {}, AfterCash: {}, DSR: {}%",
                newDeposit, newLoanAmount, afterCash, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.JEONSE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(newDeposit)
                .supportBenefit(cashInflow)
                .fundingShortage(fundingShortage)
                .summary(summary != null ? summary : "")
                .build();
    }

    private BigDecimal resolveBaseDeposit(LifecycleEventInput input) {
        try {
            return referenceService.getNationalAmount(
                    LifecycleEventType.JEONSE,
                    "JEONSE_BASE_DEPOSIT",
                    null
            );
        } catch (Exception e) {
            return new BigDecimal("300000000.00");
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