package com.via.shinvia.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserProfileRequestDto {
    private String loginEmail;

    @NotBlank(message = "이름을 입력해주세요.")
    private String userName;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(
            regexp = "^010\\d{8}$",
            message = "휴대폰 번호는 01012345678 형식으로 입력해주세요."
    )
    private String phoneNumber;

    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일을 확인해주세요.")
    private LocalDate birthDate;
}
