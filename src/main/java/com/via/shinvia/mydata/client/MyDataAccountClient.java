package com.via.shinvia.mydata.client;

import com.via.shinvia.mydata.client.dto.response.BankAccountsResponseDto;
import com.via.shinvia.mydata.config.MyDataProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MyDataAccountClient {
    private final RestClient restClient;
    private final MyDataProperties myDataProperties;

    public MyDataAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${mydata.mock-server.base-url:${mydata.mock.base-url:http://localhost:9090}}") String baseUrl,
            MyDataProperties myDataProperties
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.myDataProperties = myDataProperties;
    }

    public BankAccountsResponseDto getAccounts(String orgCode, Integer limit) {
        String effectiveOrgCode = myDataProperties.getOrgCode();
        return restClient.get().uri(uriBuilder -> uriBuilder.path("/v2/bank/accounts")
                .queryParam("org_code", effectiveOrgCode)
                .queryParam("limit", limit)
                .build()).retrieve().body(BankAccountsResponseDto.class);
    }
}
