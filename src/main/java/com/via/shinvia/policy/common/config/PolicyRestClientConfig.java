package com.via.shinvia.policy.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
// 정책상품 API 통신 설정 기능
public class PolicyRestClientConfig {

    @Bean
    public RestClient policyRestClient(
            @Value("${finance.api.connect-timeout:5s}") Duration connectTimeout,
            @Value("${finance.api.read-timeout:15s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
