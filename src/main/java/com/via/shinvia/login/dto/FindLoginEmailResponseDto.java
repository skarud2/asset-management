package com.via.shinvia.login.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FindLoginEmailResponseDto {
    private String maskedEmail;
    private List<String> providers;
}
