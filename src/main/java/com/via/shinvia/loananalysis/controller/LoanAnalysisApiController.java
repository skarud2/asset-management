package com.via.shinvia.loananalysis.controller;

import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioRequestDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioResponseDTO;
import com.via.shinvia.loananalysis.service.LoanAnalysisService;
import com.via.shinvia.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 대출분석 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-analysis")
public class LoanAnalysisApiController {

    private final LoanAnalysisService
            loanAnalysisService;

    private final CurrentUser currentUser;


    // 부채 상환순위 조회
    @GetMapping("/debt-priority")
    public ResponseEntity<
            List<DebtPriorityResponseDTO>
            > getDebtPriorities(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);

        return ResponseEntity.ok(
                loanAnalysisService
                        .getDebtPriorities(
                                userId
                        )
        );
    }


    // 유지·부분상환·대환·현금보유 비교
    @PostMapping("/scenarios")
    public ResponseEntity<
            List<LoanScenarioResponseDTO>
            > analyzeScenarios(
            Authentication authentication,
            @Valid @RequestBody
            LoanScenarioRequestDTO request
    ) {
        Long userId = currentUser.getUserId(authentication);

        return ResponseEntity.ok(
                loanAnalysisService
                        .analyzeScenarios(
                                userId,
                                request
                        )
        );
    }
}
