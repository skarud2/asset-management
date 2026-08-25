package com.via.shinvia.surplusfund.guideversion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuideVersionRenameRequest(
        @NotBlank
        @Size(max = 100)
        String guideName
) {
}
