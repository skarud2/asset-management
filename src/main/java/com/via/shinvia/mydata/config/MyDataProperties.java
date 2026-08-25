package com.via.shinvia.mydata.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "shinvia-client.mydata")
public class MyDataProperties {
    private String clientId;
    private String clientSecret;
    private String orgCode;
    private String redirectUri;
    private String appScheme = "shinvia://oauth";
}
