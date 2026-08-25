package com.via.shinvia.mydata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyDataConnectionResponseDto {
    private boolean connected;
    private LocalDateTime connectedAt;
}
