package com.via.shinvia.loan.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.finlife")
public record FinlifeProperties(
        String baseUrl,
        String authKey,
        String topFinGrpNo
) {
}
