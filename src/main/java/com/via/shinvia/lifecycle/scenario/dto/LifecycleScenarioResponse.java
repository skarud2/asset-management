package com.via.shinvia.lifecycle.scenario.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LifecycleScenarioResponse {

    private Long scenarioId;
    private String scenarioName;
    private String description;
    private LocalDate baseDate;
    private String status;
    private Integer eventCount;
}
