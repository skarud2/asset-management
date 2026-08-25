package com.via.shinvia.surplusfund.preference.controller;

// 운용 성향 설문 요청 및 결과 API CONTROLLER

import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceRequest;
import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceResponse;
import com.via.shinvia.surplusfund.preference.service.InvestmentPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/surplus-funds/preferences")
public class InvestmentPreferenceController {
    private final InvestmentPreferenceService investmentPreferenceService;

    public InvestmentPreferenceController(
            InvestmentPreferenceService investmentPreferenceService
    ) {
        this.investmentPreferenceService = investmentPreferenceService;
    }


    @PostMapping("/analyze")
    public ResponseEntity<InvestmentPreferenceResponse> analyze(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody InvestmentPreferenceRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return ResponseEntity.ok(
                investmentPreferenceService.analyzeAndSave(userId, request)
        );
    }

}
