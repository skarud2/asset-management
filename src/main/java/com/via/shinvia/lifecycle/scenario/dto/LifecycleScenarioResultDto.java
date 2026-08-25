package com.via.shinvia.lifecycle.scenario.dto;

import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleScenarioResultDto {

    private Long scenarioId;
    private Long userId;

    private LocalDateTime simulatedAt;

    private LifecycleFinancialStateDto initialState;
    private LifecycleFinancialStateDto finalState;

    private Integer eventCount;

    private BigDecimal totalEventCost;
    private BigDecimal totalSupportBenefit;
    private BigDecimal totalFundingShortage;

    private BigDecimal initialNetAsset;
    private BigDecimal finalNetAsset;
    private BigDecimal netAssetChange;

    private BigDecimal initialCashAsset;
    private BigDecimal finalCashAsset;

    private BigDecimal initialTotalDebt;
    private BigDecimal finalTotalDebt;

    private BigDecimal finalMonthlySavingCapacity;
    private BigDecimal finalDsr;

    private List<LifecycleEventSnapshotDto> eventSnapshots;
}