package com.via.shinvia.mydata.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class MyDataConnection {
    private Long connectionId;
    private Long userId;
    private ConnectionStatus connectionStatus;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private LocalDateTime updatedAt;
}
