package com.via.shinvia.surplusfund.product.etf.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class EtfClientConfig {

    @Bean(name = "etfRestTemplate")
    public RestTemplate etfRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                Duration.ofSeconds(5)
        );

        requestFactory.setReadTimeout(
                Duration.ofSeconds(10)
        );

        return new RestTemplate(requestFactory);
    }
}