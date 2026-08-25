package com.via.shinvia.loan.catalog.client;

import com.via.shinvia.loan.catalog.config.FinlifeProperties;
import com.via.shinvia.loan.catalog.dto.external.credit.CreditResponse;
import com.via.shinvia.loan.catalog.dto.external.jeonse.JeonseResponse;
import com.via.shinvia.loan.catalog.dto.external.mortgage.MortgageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class FinlifeLoanProductClient {

    private final RestClient restClient;
    private final FinlifeProperties properties;
    private final ObjectMapper objectMapper;

    public FinlifeLoanProductClient(
            @Qualifier("finlifeRestClient") RestClient restClient,
            FinlifeProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public MortgageResponse fetchMortgage(int pageNo) {
        return get("/mortgageLoanProductsSearch.json", pageNo, MortgageResponse.class);
    }

    public JeonseResponse fetchJeonse(int pageNo) {
        return get("/rentHouseLoanProductsSearch.json", pageNo, JeonseResponse.class);
    }

    public CreditResponse fetchCredit(int pageNo) {
        return get("/creditLoanProductsSearch.json", pageNo, CreditResponse.class);
    }

    private <T> T get(String path, int pageNo, Class<T> responseType) {
        ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("auth", properties.authKey())
                        .queryParam("topFinGrpNo", properties.topFinGrpNo())
                        .queryParam("pageNo", pageNo)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException(
                    "금감원 API 응답 본문이 비어 있습니다. status=" + response.getStatusCode()
                            + ", path=" + path + ", pageNo=" + pageNo
            );
        }

        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "금감원 API JSON 변환 실패. responseType=" + responseType.getSimpleName()
                            + ", status=" + response.getStatusCode()
                            + ", body=" + abbreviate(body),
                    exception
            );
        }
    }

    private String abbreviate(String body) {
        return body.length() <= 500 ? body : body.substring(0, 500) + "...";
    }
}
