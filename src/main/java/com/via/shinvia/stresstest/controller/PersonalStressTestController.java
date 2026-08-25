package com.via.shinvia.stresstest.controller;

import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.stresstest.dto.request.StressTestRequest;
import com.via.shinvia.stresstest.dto.response.StressTestResponse;
import com.via.shinvia.stresstest.service.PersonalStressTestSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/stress-test")
public class PersonalStressTestController {

    private final PersonalStressTestSimulator simulator;
    private final CurrentUser currentUser;

    public PersonalStressTestController(PersonalStressTestSimulator simulator, CurrentUser currentUser) {
        this.simulator = simulator;
        this.currentUser = currentUser;
    }

    @GetMapping("/personal")
    public ResponseEntity<StressTestResponse> getPersonalStressTest(
            Authentication authentication,
            @RequestParam(defaultValue = "1.0") BigDecimal rateDeltaPercent,
            @RequestParam(defaultValue = "20") BigDecimal incomeDropPercent,
            @RequestParam(defaultValue = "0") BigDecimal unexpectedExpenseAmount,
            @RequestParam(defaultValue = "24") Integer simulationMonths
    ) {
        Long userId = currentUser.getUserId(authentication);
        StressTestResponse response = simulator.simulate(
                new StressTestRequest(userId, rateDeltaPercent, incomeDropPercent, unexpectedExpenseAmount, simulationMonths)
        );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }
}
