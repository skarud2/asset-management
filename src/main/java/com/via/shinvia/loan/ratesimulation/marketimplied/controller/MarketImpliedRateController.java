package com.via.shinvia.loan.ratesimulation.marketimplied.controller;

import com.via.shinvia.loan.ratesimulation.marketimplied.dto.response.MarketImpliedSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.marketimplied.service.MarketImpliedRateSimulator;
import com.via.shinvia.marketdata.NoYieldCurveDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class MarketImpliedRateController {

    private final MarketImpliedRateSimulator marketImpliedRateSimulator;

    public MarketImpliedRateController(MarketImpliedRateSimulator marketImpliedRateSimulator) {
        this.marketImpliedRateSimulator = marketImpliedRateSimulator;
    }

    @GetMapping("/{loanId}/market-implied-simulation")
    public ResponseEntity<MarketImpliedSimulationResponse> getMarketImpliedSimulation(@PathVariable Long loanId) {
        return ResponseEntity.ok(marketImpliedRateSimulator.simulate(loanId));
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LoanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({InvalidLoanStatusException.class, IllegalArgumentException.class, NoYieldCurveDataException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }
}
