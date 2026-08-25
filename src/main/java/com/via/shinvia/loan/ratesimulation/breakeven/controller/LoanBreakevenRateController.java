package com.via.shinvia.loan.ratesimulation.breakeven.controller;

import com.via.shinvia.loan.ratesimulation.breakeven.dto.request.BreakevenRateRequest;
import com.via.shinvia.loan.ratesimulation.breakeven.dto.response.BreakevenRateResponse;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.breakeven.exception.UnsupportedThresholdTypeException;
import com.via.shinvia.loan.ratesimulation.breakeven.service.LoanBreakevenRateService;
import com.via.shinvia.loan.ratesimulation.breakeven.type.ThresholdType;
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
public class LoanBreakevenRateController {

    private final LoanBreakevenRateService loanBreakevenRateService;

    public LoanBreakevenRateController(LoanBreakevenRateService loanBreakevenRateService) {
        this.loanBreakevenRateService = loanBreakevenRateService;
    }

    @GetMapping("/{loanId}/breakeven-rate")
    public ResponseEntity<BreakevenRateResponse> getBreakevenRate(
            @PathVariable Long loanId,
            @RequestParam ThresholdType thresholdType,
            @RequestParam BigDecimal thresholdValue
    ) {
        BreakevenRateResponse response = loanBreakevenRateService.calculate(
                new BreakevenRateRequest(loanId, thresholdType, thresholdValue)
        );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LoanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({InvalidLoanStatusException.class, UnsupportedThresholdTypeException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }
}
