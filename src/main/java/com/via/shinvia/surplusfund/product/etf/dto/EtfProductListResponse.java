package com.via.shinvia.surplusfund.product.etf.dto;


import java.time.LocalDate;
import java.util.List;

public record EtfProductListResponse(
        LocalDate dataBaseDate,
        int count,
        List<EtfProductResponse> products,
        String notice
) {
}

