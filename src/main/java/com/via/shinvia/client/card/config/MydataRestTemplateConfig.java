package com.via.shinvia.client.card.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class MydataRestTemplateConfig {

    private final MockServerProperties mockServerProperties;

    @Bean
    public RestTemplate mydataRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(mockServerProperties.getConnectTimeout()));
        requestFactory.setReadTimeout(Duration.ofMillis(mockServerProperties.getReadTimeout()));

        return new RestTemplate(requestFactory);
    }
}
