package com.via.shinvia.surplusfund.guideversion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GuideVersionSummaryResponse(
        Long surplusFundGuideVersionId,
        int guideVersionNo,
        String guideName,
        BigDecimal operationAmount,
        String investmentStyle,
        int selectedEtfCount,
        LocalDateTime completedAt
) {
}
