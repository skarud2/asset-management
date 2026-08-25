package com.via.shinvia.lifecycle.scenario.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LifecycleScenarioRecord {
    private Long lifecycleScenarioId;
    private Long userId;
    private String scenarioName;
    private String description;
    private LocalDate baseDate;
    private String status;
}
