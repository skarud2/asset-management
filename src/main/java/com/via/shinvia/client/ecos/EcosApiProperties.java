package com.via.shinvia.client.ecos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ecos.api")
public class EcosApiProperties {

    private String key;
    private String baseUrl;
    private int connectTimeout = 3000;
    private int readTimeout = 5000;
}
