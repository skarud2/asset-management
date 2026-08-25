package com.via.shinvia.client.ecos;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class EcosRestTemplateConfig {

    private final EcosApiProperties ecosApiProperties;

    @Bean
    public RestTemplate ecosRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(ecosApiProperties.getConnectTimeout()));
        requestFactory.setReadTimeout(Duration.ofMillis(ecosApiProperties.getReadTimeout()));

        return new RestTemplate(requestFactory);
    }
}
