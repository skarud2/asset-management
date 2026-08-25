package com.via.shinvia.lifecycle.controller;

import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.mapper.LoanAccountAnalysisMapper;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [생애주기 시뮬레이션용 사용자 대출 조회 API]
 * 사용자가 보유한 대출 목록(대출명, 잔액, 금리, 상환방식, 만기일)을 불러와
 * 대출 상환 이벤트 및 시뮬레이션에서 선택할 수 있도록 지원한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/loans")
public class LifecycleLoanController {

    private final LoanAccountAnalysisMapper loanAccountAnalysisMapper;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<LoanAccountAnalysisDTO>> getActiveLoans(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<LoanAccountAnalysisDTO> loans = loanAccountAnalysisMapper.findActiveLoansByUserId(userId);
        return ResponseEntity.ok(loans != null ? loans : List.of());
    }
}
