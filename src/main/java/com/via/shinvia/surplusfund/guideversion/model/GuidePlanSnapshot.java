package com.via.shinvia.surplusfund.guideversion.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GuidePlanSnapshot {

    private Long surplusFundPlanId;
    private Long userId;
    private BigDecimal operationAmount;
    private String investmentStyle;
    private String ruleVersion;
    private LocalDateTime confirmedAt;
}
