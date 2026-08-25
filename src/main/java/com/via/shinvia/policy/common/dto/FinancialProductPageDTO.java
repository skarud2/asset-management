package com.via.shinvia.policy.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
// 금융상품 페이징정보 전달 기능
public class FinancialProductPageDTO {

    private List<FinancialProductDTO> products;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
