package com.via.shinvia.client.card.controller;

import com.via.shinvia.client.card.billdetail.MydataCardBillDetailClient;
import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.controller.dto.CardTransactionSyncResponse;
import com.via.shinvia.service.mydata.CardSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목서버 카드 청구상세 API를 호출해 card_transaction에 저장하는 과정을 Postman 등으로 수동 실행/검증하기 위한 컨트롤러.
 * 카드 목록 동기화(/list)는 마이데이터 연동 화면(MyDataViewController)에서 자동 저장하도록 옮겨서 제거함.
 */
@Slf4j
@RestController
@RequestMapping("/api/cards/sync")
@RequiredArgsConstructor
public class CardSyncController {

    private final MydataCardBillDetailClient cardBillDetailClient;
    private final CardSyncService cardSyncService;

    @RequestMapping(value = "/transactions", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CardTransactionSyncResponse> syncCardTransactions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String orgCode,
            @RequestParam String chargeMonth,
            @RequestParam(required = false) String seqno,
            @RequestParam(defaultValue = "20") Integer limit) {

        String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";

        CardBillDetailResponse response = cardBillDetailClient.getCardBillDetails(extractAccessToken(token), CardBillDetailRequest.builder()
                .orgCode(orgCode)
                .chargeMonth(chargeMonth)
                .seqno(seqno)
                .limit(limit)
                .build());

        var saved = cardSyncService.saveCardTransactions(response);
        return ResponseEntity.ok(new CardTransactionSyncResponse(response.getBillDetailList(), saved));
    }

    private String extractAccessToken(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return authorizationHeader;
    }
}
