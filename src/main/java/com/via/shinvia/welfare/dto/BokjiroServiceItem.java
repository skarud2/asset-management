package com.via.shinvia.welfare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BokjiroServiceItem {

    private String servId;
    private String servNm;
    private String jurMnofNm;
    private String jurOrgNm;
    private String inqNum;
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
}
