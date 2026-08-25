package com.via.shinvia.policy.recommendation.common.util;

import java.util.Locale;

// 지역명 비교를 위한 값 정규화 기능
public final class RegionNormalizer {

    private RegionNormalizer() {
    }

    public static String normalizeSido(String region) {
        if (region == null || region.isBlank()) {
            return "";
        }

        String value = region.trim();

        if (value.contains("서울")) return "서울";
        if (value.contains("부산")) return "부산";
        if (value.contains("대구")) return "대구";
        if (value.contains("인천")) return "인천";
        if (value.contains("광주")) return "광주";
        if (value.contains("대전")) return "대전";
        if (value.contains("울산")) return "울산";
        if (value.contains("세종")) return "세종";
        if (value.contains("경기")) return "경기";
        if (value.contains("강원")) return "강원";
        if (value.contains("충북") || value.contains("충청북")) return "충북";
        if (value.contains("충남") || value.contains("충청남")) return "충남";
        if (value.contains("전북") || value.contains("전라북")) return "전북";
        if (value.contains("전남") || value.contains("전라남")) return "전남";
        if (value.contains("경북") || value.contains("경상북")) return "경북";
        if (value.contains("경남") || value.contains("경상남")) return "경남";
        if (value.contains("제주")) return "제주";

        return value
                .replace("특별자치도", "")
                .replace("특별자치시", "")
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("도", "")
                .trim();
    }

    public static String normalizeSigungu(String region) {
        if (region == null || region.isBlank()) {
            return "";
        }

        return region.trim().replaceAll("\\s+", "");
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#40;", "(")
                .replace("&#41;", ")")
                .replace("&amp;", "&")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.KOREAN);
    }
}
