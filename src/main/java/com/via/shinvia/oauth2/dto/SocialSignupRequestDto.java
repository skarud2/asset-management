package com.via.shinvia.oauth2.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class SocialSignupRequestDto {

    @NotBlank(message="이름은 필수입니다.")
    private String userName;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^010\\d{8}$",
            message = "휴대폰 번호는 01012345678 형식으로 입력해주세요.")
    private String phoneNumber;

    @NotNull
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;
}