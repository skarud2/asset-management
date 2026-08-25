package com.via.shinvia.lifecycle.scenario.controller;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.scenario.service.LifecycleSimulationService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioResultRecord;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/scenarios")
public class LifecycleSimulationController {

    private final LifecycleSimulationService lifecycleSimulationService;
    private final CurrentUser currentUser;

    @PostMapping("/{scenarioId:\\d+}/simulate")
    public ResponseEntity<LifecycleScenarioResultDto> simulate(
            Authentication authentication,
            @PathVariable Long scenarioId,
            @RequestBody(required = false) LifecycleBaseStateDto baseState
    ) {
        Long userId = currentUser.getUserId(authentication);
        String loginEmail = authentication != null
                ? authentication.getName()
                : null;

        LifecycleScenarioResultDto result =
                lifecycleSimulationService.simulate(
                        userId,
                        loginEmail,
                        scenarioId,
                        baseState
                );

        return ResponseEntity.ok(result);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{scenarioId:\\d+}/result")
    public ResponseEntity<LifecycleScenarioResultDto> getSimulationResult(
            Authentication authentication,
            @PathVariable Long scenarioId
    ) {
        Long userId = currentUser.getUserId(authentication);
        LifecycleScenarioResultDto result =
                lifecycleSimulationService.getSimulationResult(userId, scenarioId);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{scenarioId:\\d+}/complete-result")
    public ResponseEntity<Long> completeResult(
            Authentication authentication,
            @PathVariable Long scenarioId
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(lifecycleSimulationService.completeSimulationResult(userId, scenarioId));
    }

    @org.springframework.web.bind.annotation.GetMapping("/results")
    public ResponseEntity<List<LifecycleScenarioResultRecord>> savedResults(
            Authentication authentication
    ) {
        return ResponseEntity.ok(lifecycleSimulationService.getSavedResults(
                currentUser.getUserId(authentication)));
    }

    @org.springframework.web.bind.annotation.GetMapping("/results/{resultId:\\d+}")
    public ResponseEntity<LifecycleScenarioResultDto> savedResult(
            Authentication authentication,
            @PathVariable Long resultId
    ) {
        LifecycleScenarioResultDto result = lifecycleSimulationService.getSavedResult(
                currentUser.getUserId(authentication), resultId);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/results/{resultId:\\d+}")
    public ResponseEntity<Void> deleteSavedResult(
            Authentication authentication,
            @PathVariable Long resultId
    ) {
        lifecycleSimulationService.deleteSavedResult(
                currentUser.getUserId(authentication), resultId
        );
        return ResponseEntity.noContent().build();
    }
}
