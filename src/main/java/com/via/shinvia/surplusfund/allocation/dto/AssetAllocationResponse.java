package com.via.shinvia.surplusfund.allocation.dto;

import com.via.shinvia.surplusfund.allocation.model.AssetType;
import java.math.BigDecimal;

// 자산별 비율과 실제 배정금액을 반환
public record AssetAllocationResponse(
        AssetType assetType,
        BigDecimal ratio,
        BigDecimal amount
) {
}

