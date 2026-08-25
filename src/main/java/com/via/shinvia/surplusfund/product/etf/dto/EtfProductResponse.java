package com.via.shinvia.surplusfund.product.etf.dto;


import com.via.shinvia.surplusfund.product.etf.model.EtfProduct;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EtfProductResponse(
        Long investmentProductId,
        String productCode,
        String isinCode,
        String productName,
        LocalDate priceBaseDate,
        BigDecimal closingPrice,
        BigDecimal previousDayChange,
        BigDecimal fluctuationRate,
        BigDecimal nav,
        BigDecimal openingPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        Long tradingVolume,
        BigDecimal tradingValue,
        BigDecimal marketCap,
        BigDecimal netAssetTotalAmount,
        String baseIndexName
) {
    public static EtfProductResponse from(EtfProduct product) {
        return new EtfProductResponse(
                product.getInvestmentProductId(),
                product.getProductCode(),
                product.getIsinCode(),
                product.getProductName(),
                product.getPriceBaseDate(),
                product.getClosingPrice(),
                product.getPreviousDayChange(),
                product.getFluctuationRate(),
                product.getNav(),
                product.getOpeningPrice(),
                product.getHighPrice(),
                product.getLowPrice(),
                product.getTradingVolume(),
                product.getTradingValue(),
                product.getMarketCap(),
                product.getNetAssetTotalAmount(),
                product.getBaseIndexName()
        );
    }
}
