package com.via.shinvia.policy.asset.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 자산형성상품 DB 검색 조건
public class AssetProductSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> targets = List.of();
    private List<String> products = List.of();
    private List<String> terms = List.of();
    private List<String> ages = List.of();
    private List<String> incomes = List.of();
    private List<String> regions = List.of();
}
