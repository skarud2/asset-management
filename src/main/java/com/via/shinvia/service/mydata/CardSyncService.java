package com.via.shinvia.service.mydata;

import com.via.shinvia.client.card.bill.MydataCardBillClient;
import com.via.shinvia.client.card.bill.request.CardBillRequest;
import com.via.shinvia.client.card.bill.response.CardBillDto;
import com.via.shinvia.client.card.bill.response.CardBillResponse;
import com.via.shinvia.client.card.billdetail.MydataCardBillDetailClient;
import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailDto;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.entity.CardBill;
import com.via.shinvia.client.card.list.MydataCardListClient;
import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardInfoDto;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.entity.CardTransaction;
import com.via.shinvia.client.card.mapper.CardMapper;
import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.service.MyDataAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * mock 서버 카드 목록/청구 상세 응답을 card_account/card_transaction 테이블에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class CardSyncService {

    private static final DateTimeFormatter PAID_DTIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CardMapper cardMapper;
    private final MyDataProperties myDataProperties;
    private final MyDataAuthService myDataAuthService;
    private final MydataCardListClient mydataCardListClient;
    private final MydataCardBillClient mydataCardBillClient;
    private final MydataCardBillDetailClient mydataCardBillDetailClient;

    // TODO(마이데이터 동의 미구현): mydataConnectionId는 userId 기준으로 찾거나 새로 만든 임시값이다.
    // MyData 동의(OAuth) 플로우가 생기면 그 결과에서 나온 connection을 쓰도록 바꿀 것.
    @Transactional
    public List<CardAccount> saveCards(CardListResponse response, Long userId) {
        List<CardInfoDto> cardList = response.getCardList();
        if (cardList == null || cardList.isEmpty()) {
            return List.of();
        }

        Long connectionId = resolveConnectionId(userId);

        List<CardAccount> saved = new ArrayList<>();
        for (CardInfoDto dto : cardList) {
            String institutionOrgCode = dto.getInstitutionId() != null ? dto.getInstitutionId() : (myDataProperties != null ? myDataProperties.getOrgCode() : null);
            Long institutionId = cardMapper.findInstitutionIdByOrgCode(institutionOrgCode);

            if (institutionId == null) {
                throw new IllegalStateException("등록되지 않은 금융기관 코드입니다: " + institutionOrgCode);
            }
            saved.add(upsertCard(userId, institutionId, connectionId, dto));
        }
        return saved;
    }

    private Long resolveConnectionId(Long userId) {
        Long connectionId = cardMapper.findConnectionIdByUserId(userId);
        if (connectionId != null) {
            return connectionId;
        }
        cardMapper.insertConnection(userId);
        return cardMapper.findConnectionIdByUserId(userId);
    }

    private CardAccount upsertCard(Long userId, Long institutionId, Long connectionId, CardInfoDto dto) {
        CardAccount existing = cardMapper.findByExternalCardKey(dto.getCardId());
        LocalDateTime now = LocalDateTime.now();

        CardAccount cardAccount = CardAccount.builder()
                .cardAccountId(existing != null ? existing.getCardAccountId() : null)
                .userId(userId)
                .institutionId(institutionId)
                .connectionId(connectionId)
                .externalCardKey(dto.getCardId())
                .cardName(dto.getCardName())
                .cardNumberMasked(dto.getCardNum())
                .updatedAt(now)
                .dataAsOfAt(now)
                .build();

        if (existing == null) {
            cardMapper.insertCardAccount(cardAccount);
        } else {
            cardMapper.updateCardAccount(cardAccount);
        }
        return cardAccount;
    }

    /**
     * 카드-005(GET /v2/card/bills/detail) 응답을 card_transaction에 upsert 한다.
     * card_id는 저장하지 않고 card_account.external_card_key 조회를 통해 card_account_id로 변환해서 저장하며,
     * total_install_cnt/cur_install_cnt/balance_amt는 card_transaction에 대응 컬럼이 없어 읽지 않는다.
     */
    @Transactional
    public List<CardTransaction> saveCardTransactions(CardBillDetailResponse response) {
        List<CardBillDetailDto> billDetailList = response.getBillDetailList();
        if (billDetailList == null || billDetailList.isEmpty()) {
            return List.of();
        }

        Map<String, Long> cardAccountIdByExternalCardKey = new HashMap<>();
        List<CardTransaction> transactions = billDetailList.stream()
                .map(dto -> toEntity(dto, cardAccountIdByExternalCardKey))
                .toList();

        cardMapper.upsertCardTransactions(transactions);
        return transactions;
    }

    private CardTransaction toEntity(CardBillDetailDto dto, Map<String, Long> cardAccountIdByExternalCardKey) {
        Long cardAccountId = cardAccountIdByExternalCardKey.computeIfAbsent(dto.getCardId(), this::findCardAccountId);

        return CardTransaction.builder()
                .cardAccountId(cardAccountId)
                .externalTransactionId(dto.getTransNo())
                .transactionAt(LocalDateTime.parse(dto.getPaidDtime(), PAID_DTIME_FORMATTER))
                .amount(dto.getPaidAmt())
                .merchantName(dto.getMerchantName())
                .build();
    }

    private Long findCardAccountId(String externalCardKey) {
        Long cardAccountId = cardMapper.findCardAccountIdByExternalCardKey(externalCardKey);
        if (cardAccountId == null) {
            throw new IllegalStateException("등록되지 않은 카드입니다: " + externalCardKey);
        }
        return cardAccountId;
    }

    @Transactional
    public List<CardBill> saveCardBills(CardBillResponse response, Long userId) {

        List<CardBillDto> billList = response.getBillList();

        if (billList == null || billList.isEmpty()) {
            return List.of();
        }

        Long connectionId = resolveConnectionId(userId);

        List<CardBill> bills = billList.stream()
                .map(dto -> CardBill.builder()
                        .userId(userId)
                        .connectionId(connectionId)
                        .seqno(dto.getSeqno())
                        .chargeAmount(dto.getChargeAmt())
                        .chargeDay(dto.getChargeDay())
                        .chargeMonth(dto.getChargeMonth())
                        .paidOutDate(dto.getPaidOutDate())
                        .build())
                .toList();

        cardMapper.upsertCardBills(bills);

        return bills;
    }

    public void syncCards(Long userId, Long connectionId) {
        String accessToken = myDataAuthService.getAccessToken(connectionId);

        //카드 목록
        CardListResponse cardResponse = mydataCardListClient.getCards(accessToken, CardListRequest.builder()
                                                                                                .orgCode(myDataProperties.getOrgCode())
                                                                                                .searchTimestamp("0")
                                                                                                .limit(100)
                                                                                                .build());

        saveCards(cardResponse, userId);

        //이번 달 카드 청구금액
        String currentMonth = YearMonth.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMM"));

        CardBillResponse billResponse = mydataCardBillClient.getCardBills(
                        accessToken,
                        CardBillRequest.builder()
                                .orgCode(myDataProperties.getOrgCode())
                                .fromMonth(currentMonth)
                                .toMonth(currentMonth)
                                .limit(100)
                                .build()
                );

        saveCardBills(billResponse, userId);

        // 최근 3개월 생활비 추정을 위한 카드 거래내역 저장
        YearMonth now = YearMonth.now();

        for (int i = 0; i <= 3; i++) {

            String chargeMonth = now.minusMonths(i)
                    .format(DateTimeFormatter.ofPattern("yyyyMM"));

            CardBillDetailResponse detailResponse =
                    mydataCardBillDetailClient.getCardBillDetails(
                            accessToken,
                            CardBillDetailRequest.builder()
                                    .orgCode(myDataProperties.getOrgCode())
                                    .chargeMonth(chargeMonth)
                                    .limit(100)
                                    .build()
                    );

            saveCardTransactions(detailResponse);
        }
    }

    public void syncCardBills(Long userId, Long connectionId) {

        String accessToken = myDataAuthService.getAccessToken(connectionId);

        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        CardBillResponse billResponse = mydataCardBillClient.getCardBills(
                                            accessToken,
                                            CardBillRequest.builder()
                                                    .orgCode(myDataProperties.getOrgCode())
                                                    .fromMonth(currentMonth)
                                                    .toMonth(currentMonth)
                                                    .limit(100)
                                                    .build()
                                    );

        saveCardBills(billResponse, userId);
    }
}
