package com.via.shinvia.surplusfund.calculation.controller;

import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.surplusfund.calculation.dto.SurplusFundCalculationSaveRequest;
import com.via.shinvia.surplusfund.calculation.service.SurplusFundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/surplus-funds/calculations")
@RequiredArgsConstructor
public class SurplusFundCalculationController {

    private final SurplusFundService surplusFundService;
    private final CurrentUser currentUser;


    @PostMapping
    public ResponseEntity<Map<String, Long>> save(
            Authentication authentication,
            @Valid @RequestBody
            SurplusFundCalculationSaveRequest request
    ) {

        Long userId = currentUser.getUserId(authentication);

        Long calculationId = surplusFundService.saveCalculation(userId, request
        );

        return ResponseEntity.ok(
                Map.of("calculationId", calculationId)
        );
    }
}