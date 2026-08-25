package com.via.shinvia.service.mydata;

import com.via.shinvia.client.card.billdetail.response.CardBillDetailDto;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.list.response.CardInfoDto;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.entity.CardTransaction;
import com.via.shinvia.client.card.mapper.CardMapper;
import com.via.shinvia.client.card.config.MockServerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CardMapper를 mock 처리해 DB 연결 없이 실행되는 단위 테스트.
 * 실제 목서버 응답 파싱은 CardMydataClientIntegrationTest에서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CardSyncServiceTest {

    @Mock
    private CardMapper cardMapper;

    @Mock
    private com.via.shinvia.mydata.config.MyDataProperties myDataProperties;

    @InjectMocks
    private CardSyncService cardSyncService;

    @Test
    void 카드_목록_동기화_신규카드는_insert() {
        when(cardMapper.findInstitutionIdByOrgCode("004")).thenReturn(1L);
        when(cardMapper.findByExternalCardKey("CARD00000001")).thenReturn(null);

        CardInfoDto dto = new CardInfoDto();
        dto.setCardId("CARD00000001");
        dto.setInstitutionId("004");
        dto.setCardNum("1234-****-****-5678");
        dto.setCardName("via 신용카드");
        dto.setCardMember("1");
        dto.setIsConsent(true);

        CardListResponse response = new CardListResponse();
        response.setCardList(List.of(dto));

        cardSyncService.saveCards(response, 100L);

        ArgumentCaptor<CardAccount> captor = ArgumentCaptor.forClass(CardAccount.class);
        verify(cardMapper).insertCardAccount(captor.capture());
        verify(cardMapper, never()).updateCardAccount(any());

        CardAccount saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getInstitutionId()).isEqualTo(1L);
        assertThat(saved.getExternalCardKey()).isEqualTo("CARD00000001");
        assertThat(saved.getCardName()).isEqualTo("via 신용카드");
        assertThat(saved.getCardNumberMasked()).isEqualTo("1234-****-****-5678");
    }

    @Test
    void 카드_목록_동기화_기존카드는_update() {
        when(cardMapper.findInstitutionIdByOrgCode("004")).thenReturn(1L);
        CardAccount existing = CardAccount.builder().cardAccountId(10L).build();
        when(cardMapper.findByExternalCardKey("CARD00000001")).thenReturn(existing);

        CardInfoDto dto = new CardInfoDto();
        dto.setCardId("CARD00000001");
        dto.setInstitutionId("004");
        dto.setCardNum("1234-****-****-5678");
        dto.setCardName("via 신용카드");

        CardListResponse response = new CardListResponse();
        response.setCardList(List.of(dto));

        cardSyncService.saveCards(response, 100L);

        ArgumentCaptor<CardAccount> captor = ArgumentCaptor.forClass(CardAccount.class);
        verify(cardMapper).updateCardAccount(captor.capture());
        verify(cardMapper, never()).insertCardAccount(any());
        assertThat(captor.getValue().getCardAccountId()).isEqualTo(10L);
    }

    @Test
    void 카드_목록_동기화_미등록_금융기관코드는_예외() {
        CardInfoDto dto = new CardInfoDto();
        dto.setCardId("CARD00000001");
        dto.setInstitutionId("999");
        when(cardMapper.findInstitutionIdByOrgCode("999")).thenReturn(null);

        CardListResponse response = new CardListResponse();
        response.setCardList(List.of(dto));

        assertThatThrownBy(() -> cardSyncService.saveCards(response, 100L))
                .isInstanceOf(IllegalStateException.class);
    }

    // 카드마다 발급 은행이 다를 수 있으므로, 응답에 institution_id가 있으면 카드별로 그 값을 그대로 써야 한다
    // (예전처럼 설정값 하나로 전체 카드에 같은 institution_id를 넣으면 안 됨).
    @Test
    void 카드마다_다른_institution_id를_각각_그대로_사용한다() {
        when(cardMapper.findInstitutionIdByOrgCode("004")).thenReturn(1L);
        when(cardMapper.findInstitutionIdByOrgCode("011")).thenReturn(2L);
        when(cardMapper.findByExternalCardKey(any())).thenReturn(null);

        CardInfoDto kbCard = new CardInfoDto();
        kbCard.setCardId("CARD00000001");
        kbCard.setInstitutionId("004");
        kbCard.setCardName("KB 카드");

        CardInfoDto nhCard = new CardInfoDto();
        nhCard.setCardId("CARD00000002");
        nhCard.setInstitutionId("011");
        nhCard.setCardName("NH 카드");

        CardListResponse response = new CardListResponse();
        response.setCardList(List.of(kbCard, nhCard));

        cardSyncService.saveCards(response, 100L);

        ArgumentCaptor<CardAccount> captor = ArgumentCaptor.forClass(CardAccount.class);
        verify(cardMapper, times(2)).insertCardAccount(captor.capture());

        List<CardAccount> saved = captor.getAllValues();
        assertThat(saved.get(0).getInstitutionId()).isEqualTo(1L);
        assertThat(saved.get(1).getInstitutionId()).isEqualTo(2L);
        verify(myDataProperties, never()).getOrgCode();
    }

    // 응답에 institution_id가 없는(레거시) 카드만 설정된 기본 org_code로 대체한다.
    @Test
    void institution_id가_없으면_기본_설정값으로_대체한다() {
        when(myDataProperties.getOrgCode()).thenReturn("004");
        when(cardMapper.findInstitutionIdByOrgCode("004")).thenReturn(1L);
        when(cardMapper.findByExternalCardKey("CARD00000001")).thenReturn(null);

        CardInfoDto dto = new CardInfoDto();
        dto.setCardId("CARD00000001");
        dto.setCardName("via 신용카드");

        CardListResponse response = new CardListResponse();
        response.setCardList(List.of(dto));

        cardSyncService.saveCards(response, 100L);

        ArgumentCaptor<CardAccount> captor = ArgumentCaptor.forClass(CardAccount.class);
        verify(cardMapper).insertCardAccount(captor.capture());
        assertThat(captor.getValue().getInstitutionId()).isEqualTo(1L);
    }

    @Test
    void 카드_청구상세_동기화_필드매핑() {
        when(cardMapper.findCardAccountIdByExternalCardKey("CARD00000001")).thenReturn(5L);

        CardBillDetailDto dto = new CardBillDetailDto();
        dto.setCardId("CARD00000001");
        dto.setTransNo("TX00000001");
        dto.setPaidDtime("20260801123456");
        dto.setPaidAmt(new BigDecimal("35000"));
        dto.setMerchantName("스타벅스 강남점");
        dto.setTotalInstallCnt(3);
        dto.setCurInstallCnt(1);
        dto.setBalanceAmt(java.math.BigInteger.valueOf(70000));

        CardBillDetailResponse response = new CardBillDetailResponse();
        response.setBillDetailList(List.of(dto));

        cardSyncService.saveCardTransactions(response);

        ArgumentCaptor<List<CardTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(cardMapper).upsertCardTransactions(captor.capture());

        List<CardTransaction> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        CardTransaction transaction = saved.get(0);
        assertThat(transaction.getCardAccountId()).isEqualTo(5L);
        assertThat(transaction.getExternalTransactionId()).isEqualTo("TX00000001");
        assertThat(transaction.getTransactionAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 12, 34, 56));
        assertThat(transaction.getAmount()).isEqualByComparingTo("35000");
        assertThat(transaction.getMerchantName()).isEqualTo("스타벅스 강남점");
    }

    @Test
    void 카드_청구상세_동기화_같은_카드는_카드계좌ID_조회를_한번만() {
        when(cardMapper.findCardAccountIdByExternalCardKey("CARD00000001")).thenReturn(5L);

        CardBillDetailDto first = billDetailOf("CARD00000001", "TX00000001");
        CardBillDetailDto second = billDetailOf("CARD00000001", "TX00000002");

        CardBillDetailResponse response = new CardBillDetailResponse();
        response.setBillDetailList(List.of(first, second));

        cardSyncService.saveCardTransactions(response);

        verify(cardMapper, times(1)).findCardAccountIdByExternalCardKey("CARD00000001");
    }

    @Test
    void 카드_청구상세_동기화_미등록_카드는_예외() {
        when(cardMapper.findCardAccountIdByExternalCardKey("CARD_UNKNOWN")).thenReturn(null);

        CardBillDetailDto dto = billDetailOf("CARD_UNKNOWN", "TX00000001");
        CardBillDetailResponse response = new CardBillDetailResponse();
        response.setBillDetailList(List.of(dto));

        assertThatThrownBy(() -> cardSyncService.saveCardTransactions(response))
                .isInstanceOf(IllegalStateException.class);
    }

    private CardBillDetailDto billDetailOf(String cardId, String transNo) {
        CardBillDetailDto dto = new CardBillDetailDto();
        dto.setCardId(cardId);
        dto.setTransNo(transNo);
        dto.setPaidDtime("20260801123456");
        dto.setPaidAmt(new BigDecimal("1000"));
        dto.setMerchantName("가맹점");
        return dto;
    }
}
