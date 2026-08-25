package com.via.shinvia.report.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportCardLayout {

    private Long id;

    private Long userId;

    private String cardKey;

    private Long refId;

    private int displayOrder;

    private LocalDateTime createdAt;
}
