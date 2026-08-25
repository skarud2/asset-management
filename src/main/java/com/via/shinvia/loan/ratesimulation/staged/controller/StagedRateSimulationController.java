package com.via.shinvia.loan.ratesimulation.staged.controller;

import com.via.shinvia.loan.ratesimulation.staged.dto.request.StagedRateSimulationRequest;
import com.via.shinvia.loan.ratesimulation.staged.dto.response.StagedRateSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class StagedRateSimulationController {

    private final StagedRateSimulator stagedRateSimulator;

    public StagedRateSimulationController(StagedRateSimulator stagedRateSimulator) {
        this.stagedRateSimulator = stagedRateSimulator;
    }

    @GetMapping("/{loanId}/staged-rate-simulation")
    public ResponseEntity<StagedRateSimulationResponse> getStagedRateSimulation(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "6") int repricingCycleMonths,
            @RequestParam BigDecimal stepDeltaPercent,
            @RequestParam(defaultValue = "5") int stepCount
    ) {
        StagedRateSimulationResponse response = stagedRateSimulator.simulate(
                new StagedRateSimulationRequest(loanId, repricingCycleMonths, stepDeltaPercent, stepCount)
        );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LoanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({InvalidLoanStatusException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }
}
