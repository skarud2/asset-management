package com.via.shinvia.welfare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "지자체 복지서비스 목록 조회 요청 DTO")
public class LocalBokjiroListRequestDTO {

    @Schema(description = "페이지 번호 (기본값: 1, 최대: 1000)", example = "1")
    @Builder.Default
    private int pageNo = 1;

    @Schema(description = "출력 건수 (기본값: 10, 최대: 500)", example = "10")
    @Builder.Default
    private int numOfRows = 10;

    @Schema(description = "검색분류 (001: 제목, 002: 내용, 003: 제목+내용)", example = "001")
    @Builder.Default
    private String srchKeyCode = "001";

    @Schema(description = "검색어", example = "청년")
    private String searchWrd;

    @Schema(description = "시도 코드/명", example = "서울특별시")
    private String ctpvNm;

    @Schema(description = "시군구 코드/명", example = "강남구")
    private String sggNm;

    @Schema(description = "생애주기 코드", example = "004")
    private String lifeArray;

    @Schema(description = "가구유형 코드", example = "010")
    private String trgterIndvdlArray;

    @Schema(description = "관심주제 코드", example = "050")
    private String intrsThemaArray;

    @Schema(description = "나이", example = "25")
    private Integer age;

    @Schema(description = "온라인 신청 가능 여부 (Y/N)", example = "Y")
    private String onapPsbltYn;

    @Schema(description = "정렬순서 (date: 최신/조회순, popular: 인기순)", example = "popular")
    private String orderBy;
}
