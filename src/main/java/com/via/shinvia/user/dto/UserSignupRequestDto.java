package com.via.shinvia.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class UserSignupRequestDto {
    @NotBlank(message="이메일은 필수입니다.")
    @Email(message="올바른 이메일 형식이 아닙니다.")
    private String loginEmail;

    @NotBlank(message="비밀번호는 필수입니다.")
    @Size(min=8, max=64, message="비밀번호는 8~64자여야 합니다.")
    private String password;

    @NotBlank(message="이름은 필수입니다.")
    private String userName;

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^010\\d{8}$",
            message = "휴대폰 번호는 01012345678 형식으로 입력해주세요.")
    private String phoneNumber;

    @NotNull
    @Past(message="생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;
}
