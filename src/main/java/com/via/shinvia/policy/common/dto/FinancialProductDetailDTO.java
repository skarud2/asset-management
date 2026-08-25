package com.via.shinvia.policy.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
// 금융상품 상세정보 전달 기능
public class FinancialProductDetailDTO {

    private String title;
    private String badge;
    private String listPath;
    private Map<String, String> summary;
    private Map<String, String> conditions;
    private Map<String, String> application;
    private String relatedSite;
}
