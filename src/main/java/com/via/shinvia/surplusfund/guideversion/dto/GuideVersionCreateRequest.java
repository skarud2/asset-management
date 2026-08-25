package com.via.shinvia.surplusfund.guideversion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuideVersionCreateRequest(
        @Size(max = 100)
        String guideName,

        @NotNull
        @Positive
        Long surplusFundCalculationId,

        @NotNull
        @Positive
        Long surplusFundPlanId,

        @NotNull
        @Size(max = 4)
        List<@NotNull @Positive Long> selectedEtfProductIds,

        @NotBlank
        @Size(max = 64)
        String idempotencyKey
) {
}
