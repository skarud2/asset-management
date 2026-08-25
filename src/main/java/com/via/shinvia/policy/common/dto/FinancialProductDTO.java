package com.via.shinvia.policy.common.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 금융상품 목록정보 전달 기능
public class FinancialProductDTO {

    private String id;
    private String searchText;
    private String title;
    private String badge;
    private String firstLabel;
    private String firstValue;
    private String secondLabel;
    private String secondValue;
    private String institution;
}
