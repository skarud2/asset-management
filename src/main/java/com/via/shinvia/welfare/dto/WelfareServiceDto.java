package com.via.shinvia.welfare.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareServiceDto {
    private Long id;
    private String servId;
    private String servNm;
    private String jurMnofNm;
    private String jurOrgNm;
    private Integer inqNum;
    private String servDgst;
    private String servDtlLink;
    private String svcfrstRegTs;
    private String lifeArray;
    private String intrsThemaArray;
    private String trgterIndvdlArray;
    private String sprtCycNm;
    private String srvPvsnNm;
    private String rprsCtadr;
    private String onapPsbltYn;

    // 상세 정보 필드
    private String wlfareInfoOutlCn;
    private String crtrYr;
    private String tgtrDtlCn;
    private String slctCritCn;
    private String alwServCn;

    private Boolean active;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
