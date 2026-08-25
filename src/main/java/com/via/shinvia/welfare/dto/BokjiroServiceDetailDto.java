package com.via.shinvia.welfare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BokjiroServiceDetailDto {

    private String servId;
    private String servNm;
    private String jurMnofNm;
    private String jurOrgNm;
    private String wlfareInfoOutlCn;
    private String crtrYr;
    private String rprsCtadr;
    private String sprtCycNm;
    private String srvPvsnNm;
    private String lifeArray;
    private String intrsThemaArray;
    private String trgterIndvdlArray;
    private String tgtrDtlCn;
    private String slctCritCn;
    private String alwServCn;
    private List<DetailSubItem> applmetList;
    private List<DetailSubItem> inqplCtadrList;
    private List<DetailSubItem> inqplHmpgReldList;
    private List<DetailSubItem> basfrmList;
    private List<DetailSubItem> baslawList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailSubItem {
        private String servSeCode;
        private String servSeDetailNm;
        private String servSeDetailLink;
    }
}
