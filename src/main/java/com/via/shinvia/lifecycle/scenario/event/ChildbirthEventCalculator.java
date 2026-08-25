package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

@Slf4j
@Component
public class ChildbirthEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.CHILDBIRTH;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal totalCost = nvl(input.getEstimatedCost());
        BigDecimal requiredAmount = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : totalCost;

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 일회성 초기 출산/조리원 비용 차감
        if (beforeCash.compareTo(requiredAmount) >= 0) {
            afterCash = beforeCash.subtract(requiredAmount);
            summary = String.format("출산 초기비용으로 약 %s원이 지출되었습니다.", formatMoney(requiredAmount));
        } else {
            fundingShortage = requiredAmount.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("출산 초기비용 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 2. 월 생활비 증가 (월 양육비 추가)
        BigDecimal additionalExpense = nvl(input.getAdditionalMonthlyExpense());
        BigDecimal newLivingExpense = nvl(beforeState.getMonthlyLivingExpense()).add(additionalExpense);

        // 3. 복지 혜택 합산 (부모급여, 아동수당 등 월 지원금 반영)
        BigDecimal monthlySupportSum = BigDecimal.ZERO;
        BigDecimal oneTimeSupportSum = BigDecimal.ZERO;

        List<LifecycleSupportDto> supports = input.getSupports();
        if (supports != null && !supports.isEmpty()) {
            for (LifecycleSupportDto support : supports) {
                if (support.getEffectType() == SupportEffectType.MONTHLY_CASH_INFLOW) {
                    monthlySupportSum = monthlySupportSum.add(nvl(support.getAmount()));
                } else {
                    oneTimeSupportSum = oneTimeSupportSum.add(nvl(support.getAmount()));
                }
            }
        }

        BigDecimal totalSupportBenefit = totalCost.subtract(requiredAmount).add(oneTimeSupportSum);
        BigDecimal newSupportIncome = nvl(beforeState.getMonthlySupportIncome()).add(monthlySupportSum);

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .monthlyLivingExpense(newLivingExpense)
                .monthlySupportIncome(newSupportIncome)
                .build();

        afterState.recalculateMonthlySavingCapacity();

        log.info("[ChildbirthEventCalculator] Event calculated. Required: {}, AdditionalExpense: {}, MonthlySupport: {}",
                requiredAmount, additionalExpense, monthlySupportSum);

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.CHILDBIRTH)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(totalSupportBenefit)
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
}

