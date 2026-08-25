package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * [2. 출산 생애주기 시뮬레이터]
 * - 산후조리비, 월 양육비, 정부 복지 혜택 매핑
 * - 값 부재 시 0 또는 빈 문자열을 기본으로 안전하게 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChildbirthEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.CHILDBIRTH;
    }

    @Override
    public LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal totalCost = resolveInitialCost(input);
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
        BigDecimal additionalExpense = input.getAdditionalMonthlyExpense() != null
                ? input.getAdditionalMonthlyExpense()
                : resolveMonthlyChildcareCost(input);
        BigDecimal newLivingExpense = nvl(beforeState.getMonthlyLivingExpense()).add(nvl(additionalExpense));

        // 3. 복지 혜택 합산 (부모급여, 아동수당 등)
        BigDecimal monthlySupportSum = BigDecimal.ZERO;
        BigDecimal oneTimeSupportSum = BigDecimal.ZERO;

        List<LifecycleSupportDto> supports = input.getSupports();
        if (supports != null && !supports.isEmpty()) {
            for (LifecycleSupportDto support : supports) {
                if (support == null) continue;
                if (support.getEffectType() == SupportEffectType.MONTHLY_CASH_INFLOW) {
                    monthlySupportSum = monthlySupportSum.add(nvl(support.getAmount()));
                } else {
                    oneTimeSupportSum = oneTimeSupportSum.add(nvl(support.getAmount()));
                }
            }
        }

        BigDecimal totalSupportBenefit = totalCost.subtract(requiredAmount).max(BigDecimal.ZERO).add(oneTimeSupportSum);
        BigDecimal newSupportIncome = nvl(beforeState.getMonthlySupportIncome()).add(monthlySupportSum);

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .monthlyLivingExpense(newLivingExpense)
                .monthlySupportIncome(newSupportIncome)
                .build();

        afterState.recalculateMonthlySavingCapacity();

        log.info("[ChildbirthEventSimulator] Simulated. Required: {}, AdditionalExpense: {}, MonthlySupport: {}",
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
                .summary(summary != null ? summary : "")
                .build();
    }

    private BigDecimal resolveInitialCost(LifecycleEventInput input) {
        if (input.getEstimatedCost() != null && input.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
            return input.getEstimatedCost();
        }

        try {
            BigDecimal baseCost = referenceService.getNationalAmount(
                    LifecycleEventType.CHILDBIRTH,
                    "POSTPARTUM_CARE_CENTER_COST",
                    null
            );
            BigDecimal initialItems = nvl(referenceService.getNationalAmount(
                    LifecycleEventType.CHILDBIRTH, "INFANT_CAR_SEAT_COST", null))
                    .add(nvl(referenceService.getNationalAmount(
                            LifecycleEventType.CHILDBIRTH, "INFANT_STROLLER_COST", null)))
                    .add(nvl(referenceService.getNationalAmount(
                            LifecycleEventType.CHILDBIRTH, "INFANT_CRIB_COST", null)))
                    .add(nvl(referenceService.getNationalAmount(
                            LifecycleEventType.CHILDBIRTH, "INFANT_OTHER_SETUP_COST", null)));
            return nvl(baseCost).add(initialItems);
        } catch (Exception e) {
            return new BigDecimal("2865000.00");
        }
    }

    private BigDecimal resolveMonthlyChildcareCost(LifecycleEventInput input) {
        try {
            BigDecimal baseCost = referenceService.getNationalAmount(
                    LifecycleEventType.CHILDBIRTH,
                    "MONTHLY_CHILDCARE_COST",
                    null
            );
            BigDecimal diaperCost = referenceService.getNationalAmount(
                    LifecycleEventType.CHILDBIRTH, "MONTHLY_DIAPER_COST", null);
            BigDecimal formulaCost = referenceService.getNationalAmount(
                    LifecycleEventType.CHILDBIRTH, "MONTHLY_FORMULA_COST", null);
            return nvl(baseCost).add(nvl(diaperCost)).add(nvl(formulaCost));
        } catch (Exception e) {
            return new BigDecimal("800000.00");
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
