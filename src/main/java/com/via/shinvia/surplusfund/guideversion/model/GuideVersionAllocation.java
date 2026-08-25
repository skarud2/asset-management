package com.via.shinvia.surplusfund.guideversion.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class GuideVersionAllocation {

    private String assetType;
    private BigDecimal allocationRatio;
    private BigDecimal allocationAmount;
}
