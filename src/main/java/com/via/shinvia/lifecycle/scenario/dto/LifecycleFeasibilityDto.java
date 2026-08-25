package com.via.shinvia.lifecycle.scenario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleFeasibilityDto {

    // READY, CAUTION, DEFER
    private String status;
    private String title;
    private String message;
    private BigDecimal cashGap;
    private Integer recommendedDelayMonths;
}
