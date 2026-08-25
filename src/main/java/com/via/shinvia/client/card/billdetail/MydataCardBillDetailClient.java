package com.via.shinvia.client.card.billdetail;

import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
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
public class MydataCardBillDetailClient {

    private final RestTemplate mydataRestTemplate;
    private final MockServerProperties mockServerProperties;

    public CardBillDetailResponse getCardBillDetails(String accessToken, CardBillDetailRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(mockServerProperties.getBaseUrl())
                .path("/v2/card/bills/detail")
                .queryParam("org_code", request.getOrgCode())
                .queryParam("charge_month", request.getChargeMonth())
                .queryParam("limit", request.getLimit());

        if (StringUtils.hasText(request.getSeqno())) {
            builder.queryParam("seqno", request.getSeqno());
        }
        if (StringUtils.hasText(request.getNextPage())) {
            builder.queryParam("next_page", request.getNextPage());
        }

        URI uri = builder.build().toUri();
        return mydataRestTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)), CardBillDetailResponse.class)
                .getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
