package com.via.shinvia.loan.catalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(FinlifeProperties.class)
public class FinlifeClientConfig {

    @Bean(name = "finlifeRestClient")
    public RestClient finlifeRestClient(FinlifeProperties properties) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException("external.finlife.base-url이 설정되지 않았습니다.");
        }
        if (properties.authKey() == null || properties.authKey().isBlank()) {
            throw new IllegalStateException("FINLIFE_API_KEY가 설정되지 않았습니다.");
        }
        if (properties.topFinGrpNo() == null || properties.topFinGrpNo().isBlank()) {
            throw new IllegalStateException("external.finlife.top-fin-grp-no가 설정되지 않았습니다.");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
