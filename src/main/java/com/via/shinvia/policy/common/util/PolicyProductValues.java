package com.via.shinvia.policy.common.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 외부 상품 필드 정리 기능
public final class PolicyProductValues {
    private PolicyProductValues() {
    }

    public static String first(Map<String, String> source, String... keys) {
        return Arrays.stream(keys)
                .map(source::get)
                .map(PolicyProductValues::clean)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static String join(String... values) {
        String result = Arrays.stream(values)
                .map(PolicyProductValues::clean)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" / "));
        return result.isBlank() ? null : result;
    }

    public static String limit(String value, int maxLength) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }

    public static String url(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.startsWith("/") || cleaned.contains(":" )
                && !cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            return null;
        }
        String candidate = cleaned.startsWith("http://") || cleaned.startsWith("https://")
                ? cleaned : "https://" + cleaned;
        try {
            URI uri = new URI(candidate);
            String host = uri.getHost();
            return host != null && host.contains(".") ? candidate : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() || "-".equals(cleaned) ? null : cleaned;
    }
}
