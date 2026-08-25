package com.via.shinvia.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequestDto (
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp="\\d{6}") String code
) {
}
