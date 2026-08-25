package com.via.shinvia.mydata.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenStatusResponse {
    private boolean hasToken;      // Redis에 토큰 존재 여부
    private Long remainingSeconds; // 남은 시간(초)
}