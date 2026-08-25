package com.via.shinvia.mydata.controller;

import com.via.shinvia.loan.account.entity.LoanAccount;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.service.mydata.LoanAccountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mydata/loans")
@RequiredArgsConstructor
public class MyDataLoanController {

    private final LoanAccountSyncService loanAccountSyncService;
    private final CurrentUser currentUser;

    @PostMapping("/sync")
    public ResponseEntity<List<LoanAccount>> syncLoans(
            Authentication authentication
    ) {

        /*
         * 로그인 사용자
         */
        Long userId =
                currentUser.getUserId(authentication);

        log.info(
                "[Loan Sync Controller] 보유대출 동기화 요청 - userId={}",
                userId
        );

        List<LoanAccount> loans =
                loanAccountSyncService
                        .syncLoans(userId);

        return ResponseEntity.ok(loans);
    }

    /**
     * 이미 동기화되어 저장된 보유 대출 목록 조회 (목서버 재호출 없음).
     * 대출 시뮬레이션 화면의 "내 보유 대출" 위젯에서 사용한다.
     */
    @GetMapping
    public ResponseEntity<List<LoanAccount>> myLoans(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(loanAccountSyncService.getMyLoans(userId));
    }
}