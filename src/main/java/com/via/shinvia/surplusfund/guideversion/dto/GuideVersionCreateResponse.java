package com.via.shinvia.surplusfund.guideversion.dto;

import java.time.LocalDateTime;

public record GuideVersionCreateResponse(
        Long surplusFundGuideVersionId,
        int guideVersionNo,
        String guideName,
        int selectedEtfCount,
        LocalDateTime completedAt
) {
}
