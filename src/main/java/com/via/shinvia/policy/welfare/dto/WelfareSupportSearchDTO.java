package com.via.shinvia.policy.welfare.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 복합지원상품 DB 검색 조건
public class WelfareSupportSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> targets = List.of();
    private List<String> ages = List.of();
}
