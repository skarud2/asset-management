package com.via.shinvia.surplusfund.guideversion.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GuideVersionEtfSnapshot {

    private Long surplusFundGuideEtfSnapshotId;
    private Long surplusFundGuideVersionId;
    private Long sourceInvestmentProductId;
    private int selectionOrder;

    private String productCode;
    private String isinCode;
    private String productName;
    private String providerName;
    private String category;
    private String sourceType;
    private LocalDate priceBaseDate;
    private BigDecimal closingPrice;
    private BigDecimal previousDayChange;
    private BigDecimal fluctuationRate;
    private BigDecimal nav;
    private BigDecimal openingPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long tradingVolume;
    private BigDecimal tradingValue;
    private Long listedShareCount;
    private BigDecimal marketCap;
    private BigDecimal netAssetTotalAmount;
    private String baseIndexName;
    private BigDecimal baseIndexClose;
    private LocalDateTime productLastSyncedAt;
    private LocalDateTime capturedAt;
}
