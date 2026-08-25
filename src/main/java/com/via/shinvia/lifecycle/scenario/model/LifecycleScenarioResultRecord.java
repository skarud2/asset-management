package com.via.shinvia.lifecycle.scenario.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
public class LifecycleScenarioResultRecord {
    private Long lifecycleScenarioResultId;
    private Long lifecycleScenarioId;
    private String scenarioName;
    private LocalDateTime simulatedAt;
    private Integer eventCount;
    private BigDecimal totalEventCost;
    private BigDecimal totalFundingShortage;
    private BigDecimal initialNetAsset;
    private BigDecimal finalNetAsset;
    private BigDecimal netAssetChange;
    private BigDecimal finalMonthlySavingCapacity;
    private BigDecimal finalDsr;
    private String orderedExpenseAmountsJson;
    private String orderedEventCostsJson;
    private String oneTimeCostBreakdownJson;
    private String monthlyExpenseBreakdownJson;
    private String detailedAnalysisJson;
}
