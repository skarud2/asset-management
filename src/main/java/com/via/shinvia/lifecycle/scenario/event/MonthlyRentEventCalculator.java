package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * [월세 이벤트 계산기]
 * 1. 기존에 묶여있던 주거 보증금(전세/월세)이 있다면 현금으로 회수
 * 2. 신규 월세 보증금 차감 (부족 시 부족자금 산출) 및 housingAsset(보증금) 등록
 * 3. 매월 나가는 월 주거비(monthlyHousingExpense = 월세 + 관리비) 갱신
 */
@Slf4j
@Component
public class MonthlyRentEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.MONTHLY_RENT;
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

            // 기존 주담대(MORTGAGE) 완납 처리
            var mortgageLoans = updatedLoans.stream()
                    .filter(l -> l.getLoanType() != null && l.getLoanType().toUpperCase().contains("MORTGAGE"))
                    .toList();
            for (var mLoan : mortgageLoans) {
                if (mLoan.getCurrentBalance() != null) {
                    newTotalDebt = newTotalDebt.subtract(mLoan.getCurrentBalance()).max(BigDecimal.ZERO);
                }
                updatedLoans.remove(mLoan);
            }
        }

        // 기존 임차 보증금 회수
        BigDecimal previousDeposit = nvl(beforeState.getDepositAsset());
        if (previousDeposit.compareTo(BigDecimal.ZERO) == 0 && nvl(beforeState.getRealEstateAsset()).compareTo(BigDecimal.ZERO) == 0) {
            previousDeposit = nvl(beforeState.getHousingAsset());
        }
        currentCash = currentCash.add(previousDeposit);

        // 2. 신규 월세 보증금 및 월세 비용 파악
        BigDecimal newDeposit = nvl(input.getAcquiredAssetAmount());
        BigDecimal requiredCash = input.getUserRequiredAmount() != null
                ? input.getUserRequiredAmount()
                : nvl(input.getEstimatedCost());
        if (newDeposit.signum() == 0) {
            newDeposit = nvl(input.getEstimatedCost());
        }
        BigDecimal monthlyRentAndFee = nvl(input.getAdditionalMonthlyExpense());
        BigDecimal brokerageFee = maxZero(nvl(input.getEstimatedCost()).subtract(newDeposit));

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 3. 신규 보증금 지출 처리
        if (currentCash.compareTo(requiredCash) >= 0) {
            afterCash = currentCash.subtract(requiredCash);
            if (isSellingHome) {
                summary = String.format("기존 주택을 매각하고 월세 보증금 %s원과 중개보수 %s원 지출 및 월 주거비 %s원이 설정되었습니다.",
                        formatMoney(newDeposit), formatMoney(brokerageFee), formatMoney(monthlyRentAndFee));
            } else if (afterRealEstateAsset.compareTo(BigDecimal.ZERO) > 0) {
                summary = String.format("기존 주택을 보유한 채 월세 보증금 %s원과 중개보수 %s원 지출 및 월 주거비 %s원이 설정되었습니다.",
                        formatMoney(newDeposit), formatMoney(brokerageFee), formatMoney(monthlyRentAndFee));
            } else {
                summary = String.format("월세 보증금 %s원과 중개보수 %s원 지출 및 월 주거비 %s원이 설정되었습니다.",
                        formatMoney(newDeposit), formatMoney(brokerageFee), formatMoney(monthlyRentAndFee));
            }
        } else {
            fundingShortage = requiredCash.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("월세 보증금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .realEstateAsset(afterRealEstateAsset)          // 기존 주택 보유 or 0원
                .depositAsset(newDeposit)                       // 월세 보증금 등록
                .housingAsset(afterRealEstateAsset.add(newDeposit)) // 하위 호환
                .currentHousingType("MONTHLY_RENT")             // 월세 거주 형태
                .totalDebt(newTotalDebt)
                .loans(updatedLoans)
                .monthlyHousingExpense(monthlyRentAndFee)       // 매월 나갈 월 주거비(월세+관리비) 갱신
                .monthlyDebtPayment(newDebtPayment)
                .build();

        // 5. 월 저축여력 재계산 (월세가 늘어났으므로 저축여력 감소)
        afterState.recalculateMonthlySavingCapacity();

        log.info("[MonthlyRentEventCalculator] Event calculated. Deposit: {}, MonthlyRent: {}, AfterCash: {}, Shortage: {}",
                newDeposit, monthlyRentAndFee, afterCash, fundingShortage);

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.MONTHLY_RENT)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(nvl(input.getEstimatedCost()))
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary)
                .build();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal maxZero(BigDecimal val) {
        return nvl(val).max(BigDecimal.ZERO);
    }
}

