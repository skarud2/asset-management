package com.via.shinvia.account.controller;

import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailResponse;
import com.via.shinvia.account.client.MockAccountClient;
import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.request.AccountSyncRequest;
import com.via.shinvia.account.dto.response.AccountSyncResult;
import com.via.shinvia.account.service.AccountSyncService;
import com.via.shinvia.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountSyncService accountSyncService;
    private final MockAccountClient accountClient;
    private final CurrentUser currentUser;

    @Operation(summary = "계좌 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<AccountListResponse> list(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("org_code") String orgCode,
            @RequestParam(value = "next_page", required = false) String nextPage,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        AccountListResponse response = accountClient.getAccounts(authorization, orgCode, nextPage, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "수신계좌 기본정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/deposit/basic")
    public ResponseEntity<DepositBasicResponse> getDepositBasic(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DepositBasicRequest request
    ) {
        DepositBasicResponse response = accountClient.getDepositBasic(authorization, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "수신계좌 추가(상세)정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/deposit/detail")
    public ResponseEntity<DepositDetailResponse> getDepositDetail(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DepositDetailRequest request
    ) {
        DepositDetailResponse response = accountClient.getDepositDetail(authorization, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "수신계좌 거래내역 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/deposit/transactions")
    public ResponseEntity<DepositTransactionResponse> getDepositTransactions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DepositTransactionRequest request
    ) {
        DepositTransactionResponse response = accountClient.getDepositTransactions(authorization, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "계좌 동기화")
    @PostMapping("/sync")
    public ResponseEntity<AccountSyncResult> sync(
            @RequestBody AccountSyncRequest request,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);

        AccountSyncResult result = accountSyncService.sync(userId, request);
        return ResponseEntity.ok(result);
    }
}