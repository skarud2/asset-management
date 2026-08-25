package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * [1. 결혼 생애주기 시뮬레이터]
 * - 입력값 및 lifecycle_reference 기준 데이터 매핑
 * - 값 부재 시 0 또는 빈 문자열을 기본으로 안전하게 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarriageEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.MARRIAGE;
    }

    @Override
    public LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());

        // 1. 기준 총비용 및 생활수준 배율 매핑 (값 없으면 기본값 또는 0 처리)
        BigDecimal totalCost = resolveTotalCost(input);
        BigDecimal requiredAmount = input.getUserRequiredAmount() != null
                ? input.getUserRequiredAmount()
                : totalCost;

        BigDecimal supportBenefit = nvl(input.getCashInflowAmount());
        BigDecimal familySupport = nvl(input.getFamilySupportAmount());

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 2. 자금 충분 여부 계산
        if (beforeCash.compareTo(requiredAmount) >= 0) {
            afterCash = beforeCash.subtract(requiredAmount);
            summary = String.format("결혼 준비비로 약 %s원이 지출되었습니다.", formatMoney(requiredAmount));
        } else {
            fundingShortage = requiredAmount.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("결혼 준비 시 자금이 약 %s원 부족합니다.", formatMoney(fundingShortage));
        }

        // 3. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .build();

        afterState.recalculateMonthlySavingCapacity();

        log.info("[MarriageEventSimulator] Simulated. BeforeCash: {}, Required: {}, AfterCash: {}, Shortage: {}",
                beforeCash, requiredAmount, afterCash, fundingShortage);

        // 4. 결과 DTO 반환
        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.MARRIAGE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(supportBenefit)
                .fundingShortage(fundingShortage)
                .summary(summary != null ? summary : "")
                .build();
    }

    private BigDecimal resolveTotalCost(LifecycleEventInput input) {
        if (input.getEstimatedCost() != null && input.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
            return input.getEstimatedCost();
        }

        try {
            // 1. 지역별 1인당 식대 단가 조회
            BigDecimal mealPrice = referenceService.getRegionalAmount(
                    LifecycleEventType.MARRIAGE,
                    "MEAL_COST",
                    null,
                    null,
                    null
            );

            // 2. 지역별 스드메 및 예식장 기본 패키지 비용 조회
            BigDecimal hallPackageCost = referenceService.getRegionalAmount(
                    LifecycleEventType.MARRIAGE,
                    "WEDDING_HALL_PACKAGE_COST",
                    null,
                    null,
                    input.getLifestyleLevel() != null ? input.getLifestyleLevel() : LifestyleLevel.AVERAGE
            );

            // 3. 생활수준 배율 적용
            BigDecimal multiplier = referenceService.getNationalRate(
                    LifecycleEventType.MARRIAGE,
                    "LIFESTYLE_COST_MULTIPLIER",
                    input.getLifestyleLevel() != null ? input.getLifestyleLevel() : LifestyleLevel.AVERAGE
            );

            BigDecimal adjustedHallCost = nvl(hallPackageCost).multiply(nvlRate(multiplier));
            BigDecimal totalMealCost = nvl(mealPrice).multiply(BigDecimal.valueOf(200)); // 기본 200명 기준

            return adjustedHallCost.add(totalMealCost);
        } catch (Exception e) {
            log.warn("[MarriageEventSimulator] Failed to resolve reference, fallback to default", e);
            return new BigDecimal("21390000.00");
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

    private BigDecimal nvlRate(BigDecimal val) {
        return val != null ? val : BigDecimal.ONE;
    }
}