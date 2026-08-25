package com.via.shinvia.loan.ratesimulation.historical.controller;

import com.via.shinvia.client.ecos.EcosApiException;
import com.via.shinvia.loan.ratesimulation.historical.dto.request.HistoricalRateReplayRequest;
import com.via.shinvia.loan.ratesimulation.historical.dto.response.HistoricalRateReplayResponse;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.historical.exception.NoHistoricalRateDataException;
import com.via.shinvia.loan.ratesimulation.historical.service.HistoricalRateReplaySimulator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class HistoricalRateReplayController {

    private final HistoricalRateReplaySimulator historicalRateReplaySimulator;

    public HistoricalRateReplayController(HistoricalRateReplaySimulator historicalRateReplaySimulator) {
        this.historicalRateReplaySimulator = historicalRateReplaySimulator;
    }

    @GetMapping("/{loanId}/historical-rate-replay")
    public ResponseEntity<HistoricalRateReplayResponse> getHistoricalRateReplay(
            @PathVariable Long loanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        HistoricalRateReplayResponse response = historicalRateReplaySimulator.replay(
                new HistoricalRateReplayRequest(loanId, startDate, endDate)
        );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LoanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({InvalidLoanStatusException.class, IllegalArgumentException.class, NoHistoricalRateDataException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EcosApiException.class)
    public ResponseEntity<Map<String, String>> handleUpstreamFailure(EcosApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", ex.getMessage()));
    }
}
