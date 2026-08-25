package com.via.shinvia.client.card.basic;

import com.via.shinvia.client.card.basic.request.CardBasicRequest;
import com.via.shinvia.client.card.basic.response.CardBasicResponse;
import com.via.shinvia.client.card.config.MockServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class MydataCardBasicClient {

    private final RestTemplate mydataRestTemplate;
    private final MockServerProperties mockServerProperties;

    public CardBasicResponse getCardBasic(String accessToken, CardBasicRequest request) {
        URI uri = UriComponentsBuilder.fromUriString(mockServerProperties.getBaseUrl())
                .path("/v2/card/cards/{cardId}")
                .queryParam("org_code", request.getOrgCode())
                .queryParam("search_timestamp", request.getSearchTimestamp())
                .buildAndExpand(request.getCardId())
                .toUri();

        return mydataRestTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)), CardBasicResponse.class)
                .getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
