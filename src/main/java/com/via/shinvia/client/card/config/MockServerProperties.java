package com.via.shinvia.client.card.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mydata.mock")
public class MockServerProperties {

    private String baseUrl;
    private int connectTimeout = 3000;
    private int readTimeout = 5000;
    private String orgCode;
}
