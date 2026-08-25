package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;

/**
 * [전세 이벤트 계산기]
 * 1. 기존 주거 보증금(월세/전세) 회수 -> 현금 환원
 * 2. 신규 전세보증금 중 자기자금 차감 (부족 시 fundingShortage)
 * 3. 신규 전세보증금 총액을 housingAsset(보증금 자산)에 등록
 * 4. 전세대출 발생 시 만기일시상환(이자만 납부) 월 상환액 및 DSR 반영
 * 5. 월 주거비는 순수 관리비(additionalMonthlyExpense)로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JeonseEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.JEONSE;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        // 1. 기존 주택 처분 및 기존 보증금 회수 처리
        BigDecimal currentCash = nvl(beforeState.getCashAsset());
        BigDecimal afterRealEstateAsset = nvl(beforeState.getRealEstateAsset());
        BigDecimal newTotalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal newDebtPayment = nvl(beforeState.getMonthlyDebtPayment());

        java.util.List<com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto> updatedLoans = new java.util.ArrayList<>();
        if (beforeState.getLoans() != null) {
            for (var l : beforeState.getLoans()) {
                if (l != null) updatedLoans.add(l);
            }
        }

        // 기존 소유 주택이 있는 경우: 매각 선택 여부 확인
        boolean isSellingHome = Boolean.FALSE.equals(input.getKeepExistingHome()) 
                && afterRealEstateAsset.compareTo(BigDecimal.ZERO) > 0;

        if (isSellingHome) {
            // 집 매각 -> 매각대금 현금 유입
            currentCash = currentCash.add(afterRealEstateAsset);
            afterRealEstateAsset = BigDecimal.ZERO;

            // 기존 주담대(MORTGAGE)가 있다면 매각 대금으로 완납 처리
            var mortgageLoans = updatedLoans.stream()
                    .filter(l -> l.getLoanType() != null && l.getLoanType().toUpperCase().contains("MORTGAGE"))
                    .toList();
            for (var mLoan : mortgageLoans) {
                if (mLoan.getCurrentBalance() != null) {
                    newTotalDebt = newTotalDebt.subtract(mLoan.getCurrentBalance()).max(BigDecimal.ZERO);
                }
                updatedLoans.remove(mLoan);
            }
            // 주담대 상환액 빠진 월 상환액 재계산
            newDebtPayment = calculateLoansMonthlyPayment(updatedLoans, input.getTargetDate());
        }

        // 기존 임차 보증금 전액 회수 (현금으로 전환)
        BigDecimal previousDeposit = nvl(beforeState.getDepositAsset());
        if (previousDeposit.compareTo(BigDecimal.ZERO) == 0 && nvl(beforeState.getRealEstateAsset()).compareTo(BigDecimal.ZERO) == 0) {
            previousDeposit = nvl(beforeState.getHousingAsset());
        }
        currentCash = currentCash.add(previousDeposit);

        // 2. 신규 전세보증금 및 자기자금/대출금 파악
        BigDecimal totalJeonseDeposit = nvl(input.getAcquiredAssetAmount());
        if (totalJeonseDeposit.signum() == 0) {
            totalJeonseDeposit = nvl(input.getEstimatedCost());
        }
        BigDecimal requiredCash = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : totalJeonseDeposit;
        BigDecimal brokerageFee = maxZero(
                nvl(input.getEstimatedCost()).subtract(totalJeonseDeposit)
        );
        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal monthlyMaintenanceFee = nvl(input.getAdditionalMonthlyExpense()); // 전세 관리비

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 3. 자기자금 지출 처리
        if (currentCash.compareTo(requiredCash) >= 0) {
            afterCash = currentCash.subtract(requiredCash);
            if (isSellingHome) {
                summary = String.format("기존 주택을 매각하고 전세보증금 자기자금과 중개보수 합계 %s원(중개보수 %s원)을 지출했습니다.", formatMoney(requiredCash), formatMoney(brokerageFee));
            } else if (afterRealEstateAsset.compareTo(BigDecimal.ZERO) > 0) {
                summary = String.format("기존 주택을 보유한 채 전세보증금 자기자금과 중개보수 합계 %s원(중개보수 %s원)을 지출했습니다.", formatMoney(requiredCash), formatMoney(brokerageFee));
            } else {
                summary = String.format("전세보증금 자기자금과 중개보수 합계 %s원(중개보수 %s원)을 지출했습니다.", formatMoney(requiredCash), formatMoney(brokerageFee));
            }
        } else {
            fundingShortage = requiredCash.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("전세 자기자금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 전세자금대출 발생 처리 (기본 2년/24개월, 연 3.8% 만기일시상환)
        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            newTotalDebt = newTotalDebt.add(newLoanAmount);
            int periodMonths = input.getLoanPeriodMonths() != null ? input.getLoanPeriodMonths() : 24;
            BigDecimal rate = input.getLoanInterestRate() != null ? input.getLoanInterestRate() : new BigDecimal("3.8");

            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        newLoanAmount,
                        rate,
                        periodMonths,
                        "만기일시상환"
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    newDebtPayment = newDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                // 실패 시 간이 이자 계산 (대출금 * rate / 12)
                BigDecimal monthlyInterest = newLoanAmount.multiply(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                newDebtPayment = newDebtPayment.add(monthlyInterest);
            }

            LocalDate eventDate = input.getTargetDate() != null ? input.getTargetDate() : LocalDate.now();
            updatedLoans.add(com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto.builder()
                    .loanAccountId(System.currentTimeMillis())
                    .loanType("JEONSE")
                    .currentBalance(newLoanAmount)
                    .interestRate(rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .rateType("VARIABLE")
                    .repaymentType("만기일시상환")
                    .maturityAt(eventDate.plusMonths(periodMonths))
                    .build());
        }

        // 5. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .realEstateAsset(afterRealEstateAsset)          // 기존 주택 보유 or 0원
                .depositAsset(totalJeonseDeposit)              // 전세보증금 전액을 자산으로 등록
                .housingAsset(afterRealEstateAsset.add(totalJeonseDeposit)) // 하위 호환
                .currentHousingType("JEONSE")                  // 거주 형태는 전세
                .totalDebt(newTotalDebt)                       // 전세대출 부채 등록
                .loans(updatedLoans)                           // 대출 목록 갱신
                .monthlyHousingExpense(monthlyMaintenanceFee)  // 월세는 0원 되고 순수 관리비만 발생
                .monthlyDebtPayment(newDebtPayment)            // 전세대출 이자 상환액 반영
                .build();

        // 6. 월 저축여력 및 DSR 재계산
        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[JeonseEventCalculator] Event calculated. Deposit: {}, RequiredCash: {}, Loan: {}, NewDebtPayment: {}, Shortage: {}",
                totalJeonseDeposit, requiredCash, newLoanAmount, newDebtPayment, fundingShortage);

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.JEONSE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(nvl(input.getEstimatedCost()))
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary)
                .build();
    }

    private BigDecimal calculateLoansMonthlyPayment(java.util.List<com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto> loans, LocalDate baseDate) {
        if (loans == null || loans.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (var loan : loans) {
            if (loan.getCurrentBalance() == null || loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal ratePercent = loan.getInterestRate() != null ? loan.getInterestRate().multiply(BigDecimal.valueOf(100)) : new BigDecimal("4.0");
            try {
                var res = loanRepaymentCalculator.calculate(loan.getCurrentBalance(), ratePercent, 60, loan.getRepaymentType() != null ? loan.getRepaymentType() : "만기일시상환");
                if (res != null && res.monthlyPayment() != null) sum = sum.add(res.monthlyPayment());
            } catch (Exception ignored) {
                sum = sum.add(loan.getCurrentBalance().multiply(new BigDecimal("0.04")).divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP));
            }
        }
        return sum;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal maxZero(BigDecimal val) {
        return nvl(val).max(BigDecimal.ZERO);
    }
}

