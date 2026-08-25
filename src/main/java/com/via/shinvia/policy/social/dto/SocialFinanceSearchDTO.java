package com.via.shinvia.policy.social.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 사회연대금융상품 DB 검색 조건
public class SocialFinanceSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> categories = List.of();
    private List<String> targets = List.of();
}
