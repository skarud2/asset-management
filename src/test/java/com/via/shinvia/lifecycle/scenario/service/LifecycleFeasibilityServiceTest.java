package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleFeasibilityDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LifecycleFeasibilityServiceTest {

    private final LifecycleFeasibilityService service =
            new LifecycleFeasibilityService();

    @Test
    void returnsReadyWhenCashFlowAndDsrAreStable() {
        LifecycleFeasibilityDto result = service.assess(event(
                "0", "1500000", "1200000", "15"
        ));

        assertEquals("READY", result.getStatus());
    }

    @Test
    void recommendsDelayBasedOnFundingGapAndMonthlySaving() {
        LifecycleFeasibilityDto result = service.assess(event(
                "6000000", "1000000", "800000", "10"
        ));

        assertEquals("DEFER", result.getStatus());
        assertEquals(6, result.getRecommendedDelayMonths());
        assertEquals(new BigDecimal("6000000"), result.getCashGap());
    }

    @Test
    void recommendsPlanAdjustmentWhenSavingCapacityIsNegative() {
        LifecycleFeasibilityDto result = service.assess(event(
                "0", "500000", "-100000", "20"
        ));

        assertEquals("DEFER", result.getStatus());
        assertNull(result.getRecommendedDelayMonths());
    }

    @Test
    void doesNotRecommendDelayWhenFundingIsShortAndMonthlyCashFlowIsNegative() {
        LifecycleFeasibilityDto result = service.assess(event(
                "6000000", "1000000", "-100000", "20"
        ));

        assertEquals("DEFER", result.getStatus());
        assertEquals("초기자금이 부족하고 월 적자가 예상됩니다.", result.getTitle());
        assertEquals(new BigDecimal("6000000"), result.getCashGap());
        assertNull(result.getRecommendedDelayMonths());
    }

    @Test
    void returnsCautionWhenDsrIsBetweenThirtyAndFortyPercent() {
        LifecycleFeasibilityDto result = service.assess(event(
                "0", "1000000", "500000", "35"
        ));

        assertEquals("CAUTION", result.getStatus());
    }

    @Test
    void recommendsDelayWhenDsrIsFortyPercentOrHigher() {
        LifecycleFeasibilityDto result = service.assess(event(
                "0", "1000000", "500000", "40"
        ));

        assertEquals("DEFER", result.getStatus());
    }

    private LifecycleEventResult event(
            String shortage,
            String beforeSaving,
            String afterSaving,
            String afterDsr
    ) {
        return LifecycleEventResult.builder()
                .fundingShortage(new BigDecimal(shortage))
                .beforeState(LifecycleFinancialStateDto.builder()
                        .monthlySavingCapacity(new BigDecimal(beforeSaving))
                        .build())
                .afterState(LifecycleFinancialStateDto.builder()
                        .monthlySavingCapacity(new BigDecimal(afterSaving))
                        .dsr(new BigDecimal(afterDsr))
                        .build())
                .build();
    }
}
