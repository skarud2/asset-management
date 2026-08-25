package com.via.shinvia.lifecycle.scenario.controller;

import com.via.shinvia.lifecycle.scenario.service.LifecycleScenarioService;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioCreateRequest;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResponse;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioUpdateRequest;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/scenarios")
public class LifecycleScenarioController {

    private final LifecycleScenarioService lifecycleScenarioService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<LifecycleScenarioResponse>> getScenarios(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                lifecycleScenarioService.getScenarios(userId)
        );
    }

    @PostMapping
    public ResponseEntity<LifecycleScenarioResponse> createScenario(
            Authentication authentication,
            @RequestBody LifecycleScenarioCreateRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                lifecycleScenarioService.createScenario(userId, request)
        );
    }

    @GetMapping("/{scenarioId:\\d+}")
    public ResponseEntity<LifecycleScenarioResponse> getScenario(
            Authentication authentication,
            @PathVariable Long scenarioId
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                lifecycleScenarioService.getScenario(userId, scenarioId)
        );
    }

    @PatchMapping("/{scenarioId:\\d+}")
    public ResponseEntity<LifecycleScenarioResponse> updateScenario(
            Authentication authentication,
            @PathVariable Long scenarioId,
            @RequestBody LifecycleScenarioUpdateRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                lifecycleScenarioService.updateScenario(
                        userId,
                        scenarioId,
                        request
                )
        );
    }

    @DeleteMapping("/{scenarioId:\\d+}")
    public ResponseEntity<Void> archiveScenario(
            Authentication authentication,
            @PathVariable Long scenarioId
    ) {
        Long userId = currentUser.getUserId(authentication);
        lifecycleScenarioService.archiveScenario(userId, scenarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/current")
    public ResponseEntity<Map<String, Long>> getOrCreateCurrent(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        Long scenarioId =
                lifecycleScenarioService.getOrCreateActiveScenario(userId);
        return ResponseEntity.ok(Map.of("scenarioId", scenarioId));
    }
}
