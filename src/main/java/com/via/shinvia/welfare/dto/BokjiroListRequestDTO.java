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
@Schema(description = "복지로 중앙부처 복지서비스 목록 조회 요청 DTO")
public class BokjiroListRequestDTO {

    @Schema(description = "페이지 번호 (기본값: 1, 최대: 1000)", example = "1")
    @Builder.Default
    private int pageNo = 1;

    @Schema(description = "출력 건수 (기본값: 10, 최대: 500)", example = "10")
    @Builder.Default
    private int numOfRows = 10;

    @Schema(description = "검색분류 (001: 제목, 002: 내용, 003: 제목+내용)", example = "001")
    @Builder.Default
    private String srchKeyCode = "001";

    @Schema(description = "검색어", example = "산모")
    private String searchWrd;

    @Schema(description = "생애주기 코드 (001:영유아, 002:아동, 003:청소년, 004:청년, 005:중장년, 006:노년, 007:임신·출산)", example = "007")
    private String lifeArray;

    @Schema(description = "가구유형 코드 (010:저소득, 020:장애인, 030:한부모·다문화, 040:조손, 050:다자녀, 060:보훈, 070:기타)", example = "010")
    private String trgterIndvdlArray;

    @Schema(description = "관심주제 코드 (010:신체건강, 020:정신건강, 030:생활지원, 040:주거, 050:일자리, 060:문화·여가, 070:안전·위기, 080:임신·출산, 090:영유아, 100:아동·청소년, 110:노년, 120:장애, 130:서민금융)", example = "010")
    private String intrsThemaArray;

    @Schema(description = "나이", example = "20")
    private Integer age;

    @Schema(description = "온라인 신청 가능 여부 (Y/N)", example = "Y")
    private String onapPsbltYn;

    @Schema(description = "정렬순서 (date: 최신/조회순, popular: 인기순)", example = "popular")
    private String orderBy;
}
