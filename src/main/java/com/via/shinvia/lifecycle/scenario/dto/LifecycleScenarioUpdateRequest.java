package com.via.shinvia.lifecycle.scenario.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LifecycleScenarioUpdateRequest {

    private String scenarioName;
    private String description;
    private String status;
}
