package com.via.shinvia.lifecycle.scenario.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LifecycleScenarioCreateRequest {

    private String scenarioName;
    private String description;
}
