package com.via.shinvia.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FindLoginEmailRequestDto {
    @NotBlank(message = "이름을 입력해주세요.")
    private String userName;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    private String phoneNumber;
}
