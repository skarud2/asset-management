package com.via.shinvia.welfare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "지자체 복지서비스 목록 응답 DTO")
public class LocalBokjiroListResponseDTO {

    @Schema(description = "전체 데이터 수", example = "3000")
    private int totalCount;

    @Schema(description = "페이지 번호", example = "1")
    private int pageNo;

    @Schema(description = "한 페이지 결과 수", example = "10")
    private int numOfRows;

    @Schema(description = "결과 코드", example = "0")
    private String resultCode;

    @Schema(description = "결과 메시지", example = "SUCCESS")
    private String resultMessage;

    @Schema(description = "지자체 복지서비스 목록")
    private List<LocalWelfareItem> servList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "지자체 복지서비스 목록 항목")
    public static class LocalWelfareItem {
        @Schema(description = "서비스 ID", example = "WLF00002000")
        private String servId;

        @Schema(description = "서비스명", example = "청년 월세 한시 특별지원")
        private String servNm;

        @Schema(description = "소관부처/지자체명", example = "서울특별시")
        private String jurMnofNm;

        @Schema(description = "소관조직명", example = "청년사업단")
        private String jurOrgNm;

        @Schema(description = "시도명", example = "서울특별시")
        private String ctpvNm;

        @Schema(description = "시군구명", example = "강남구")
        private String sggNm;

        @Schema(description = "조회수", example = "1234")
        private String inqNum;

        @Schema(description = "서비스 요약")
        private String servDgst;

        @Schema(description = "서비스 상세링크")
        private String servDtlLink;

        @Schema(description = "서비스 등록일", example = "20220122")
        private String svcfrstRegTs;

        @Schema(description = "생애주기")
        private String lifeArray;

        @Schema(description = "관심주제")
        private String intrsThemaArray;

        @Schema(description = "가구유형")
        private String trgterIndvdlArray;

        @Schema(description = "지원주기", example = "월")
        private String sprtCycNm;

        @Schema(description = "제공유형", example = "현금지급")
        private String srvPvsnNm;

        @Schema(description = "문의처", example = "02-120")
        private String rprsCtadr;

        @Schema(description = "온라인 신청 가능 여부 (Y/N)", example = "Y")
        private String onapPsbltYn;
    }
}
