package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Slf4j
@Component
public class MarriageEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.MARRIAGE;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        // 본인 부담 필요자금 (없으면 총비용 적용)
        BigDecimal requiredAmount = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : nvl(input.getEstimatedCost());
        
        BigDecimal totalCost = nvl(input.getEstimatedCost());
        // 추천 후보(NEEDS_CONFIRMATION)와 배우자·가족 분담금은 공공 지원 혜택이 아니다.
        BigDecimal supportBenefit = nvl(input.getCashInflowAmount());

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 자금 충분 여부 계산
        if (beforeCash.compareTo(requiredAmount) >= 0) {
            afterCash = beforeCash.subtract(requiredAmount);
            summary = String.format("결혼 준비비로 약 %s원이 지출되었습니다.", formatMoney(requiredAmount));
        } else {
            fundingShortage = requiredAmount.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("결혼 준비 시 자금이 약 %s원 부족합니다.", formatMoney(fundingShortage));
        }

        // 2. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .build();

        afterState.recalculateMonthlySavingCapacity();

        log.info("[MarriageEventCalculator] Event calculated. BeforeCash: {}, Required: {}, AfterCash: {}, Shortage: {}",
                beforeCash, requiredAmount, afterCash, fundingShortage);

        // 3. 결과 DTO 반환
        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.MARRIAGE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(supportBenefit)
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

