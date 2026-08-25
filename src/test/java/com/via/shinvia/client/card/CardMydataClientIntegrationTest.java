package com.via.shinvia.client.card;

import com.via.shinvia.client.card.basic.MydataCardBasicClient;
import com.via.shinvia.client.card.basic.request.CardBasicRequest;
import com.via.shinvia.client.card.basic.response.CardBasicResponse;
import com.via.shinvia.client.card.bill.MydataCardBillClient;
import com.via.shinvia.client.card.bill.request.CardBillRequest;
import com.via.shinvia.client.card.bill.response.CardBillResponse;
import com.via.shinvia.client.card.billdetail.MydataCardBillDetailClient;
import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.list.MydataCardListClient;
import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardInfoDto;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.client.card.config.MockServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 목서버(Shinvia-mock)가 localhost:9090에서 실행 중이고 seed.sql이 적재돼 있어야 통과한다.
 * Spring 컨텍스트(DataSource 포함)를 띄우지 않고 client만 수동으로 조립해서, DB 연결 여부와
 * 무관하게 /v2/card/* 응답 파싱만 검증한다.
 */
class CardMydataClientIntegrationTest {

    private static final String BASE_URL = "http://localhost:9090";
    private static final String ACCESS_TOKEN = "mock-access-token-1000000001";
    private static final String ORG_CODE = "004";
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockServerProperties properties = new MockServerProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setConnectTimeout(3000);
        properties.setReadTimeout(5000);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeout()));
        restTemplate = new RestTemplate(requestFactory);

        assumeMockServerRunning();
    }

    private void assumeMockServerRunning() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(ACCESS_TOKEN);
            restTemplate.exchange(BASE_URL + "/v2/card/cards?org_code=" + ORG_CODE + "&limit=1",
                    HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        } catch (Exception e) {
            assumeTrue(false, "목서버(localhost:9090)가 실행 중이 아니라 테스트를 건너뜀: " + e.getMessage());
        }
    }

    @Test
    void 카드_001_목록_조회() {
        MydataCardListClient client = new MydataCardListClient(restTemplate, mockServerProperties());

        CardListResponse response = client.getCards(ACCESS_TOKEN, CardListRequest.builder()
                .orgCode(ORG_CODE)
                .searchTimestamp("0")
                .limit(10)
                .build());

        assertThat(response.getCardList()).isNotEmpty();

        CardInfoDto first = response.getCardList().get(0);
        assertThat(first.getCardId()).isNotBlank();
        assertThat(first.getCardMember()).isEqualTo("1");
        assertThat(first.getIsConsent()).isTrue();
    }

    @Test
    void 카드_002_기본정보_조회() {
        MydataCardBasicClient client = new MydataCardBasicClient(restTemplate, mockServerProperties());

        CardBasicResponse response = client.getCardBasic(ACCESS_TOKEN, CardBasicRequest.builder()
                .cardId("CARD00000001")
                .orgCode(ORG_CODE)
                .searchTimestamp("0")
                .build());

        assertThat(response.getRspCode()).isEqualTo("0000");
        assertThat(response.getCardBrand()).isNotBlank();
        assertThat(response.getAnnualFee()).isNotNull();
        assertThat(response.getIssueDate()).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(response.getIsTransPayable()).isNotNull();
        assertThat(response.getIsCashCard()).isNotNull();
        assertThat(response.getLinkedBankCode()).isNotBlank();
        assertThat(response.getAccountNum()).isNotBlank();
    }

    @Test
    void 카드_004_청구_기본정보_조회() {
        MydataCardBillClient client = new MydataCardBillClient(restTemplate, mockServerProperties());

        CardBillResponse response = client.getCardBills(ACCESS_TOKEN, CardBillRequest.builder()
                .orgCode(ORG_CODE)
                .fromMonth("202605")
                .toMonth("202607")
                .limit(10)
                .build());

        assertThat(response.getRspCode()).isEqualTo("0000");
        assertThat(response.getBillList()).isNotEmpty();
        assertThat(response.getBillList().get(0).getChargeMonth()).matches("\\d{6}");
    }

    @Test
    void 카드_005_청구_추가정보_조회() {
        MydataCardBillDetailClient client = new MydataCardBillDetailClient(restTemplate, mockServerProperties());

        CardBillDetailResponse response = client.getCardBillDetails(ACCESS_TOKEN, CardBillDetailRequest.builder()
                .orgCode(ORG_CODE)
                .chargeMonth("202607")
                .seqno("0")
                .limit(10)
                .build());

        assertThat(response.getRspCode()).isEqualTo("0000");
        assertThat(response.getBillDetailList()).isNotEmpty();
        assertThat(response.getBillDetailList().get(0).getCardId()).isNotBlank();
    }

    private MockServerProperties mockServerProperties() {
        MockServerProperties properties = new MockServerProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setConnectTimeout(3000);
        properties.setReadTimeout(5000);
        return properties;
    }
}
