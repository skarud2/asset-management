package com.via.shinvia.client.card.bill;

import com.via.shinvia.client.card.bill.request.CardBillRequest;
import com.via.shinvia.client.card.bill.response.CardBillResponse;
import com.via.shinvia.client.card.config.MockServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class MydataCardBillClient {

    private final RestTemplate mydataRestTemplate;
    private final MockServerProperties mockServerProperties;

    public CardBillResponse getCardBills(String accessToken, CardBillRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(mockServerProperties.getBaseUrl())
                .path("/v2/card/bills")
                .queryParam("org_code", request.getOrgCode())
                .queryParam("from_month", request.getFromMonth())
                .queryParam("to_month", request.getToMonth())
                .queryParam("limit", request.getLimit());

        if (StringUtils.hasText(request.getNextPage())) {
            builder.queryParam("next_page", request.getNextPage());
        }

        URI uri = builder.build().toUri();
        return mydataRestTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)), CardBillResponse.class)
                .getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
