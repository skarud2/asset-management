package com.via.shinvia.surplusfund.guideversion.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GuideVersionRecord {

    private Long surplusFundGuideVersionId;
    private Long userId;
    private int guideVersionNo;
    private String guideName;
    private Long surplusFundCalculationId;
    private Long surplusFundPlanId;
    private int selectedEtfCount;
    private String snapshotSchemaVersion;
    private String idempotencyKey;
    private LocalDateTime completedAt;
    private LocalDateTime nameUpdatedAt;
    private LocalDateTime createdAt;

    private BigDecimal operationAmount;
    private String investmentStyle;
}
