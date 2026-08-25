package com.via.shinvia.policy.asset.controller;

import com.via.shinvia.policy.asset.dto.AssetProductSearchDTO;
import com.via.shinvia.policy.asset.service.AssetProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Slf4j
// 자산형성상품 화면 및 상세조회 기능
public class AssetProductController {
    private final AssetProductService service;

    @GetMapping("/asset-products")
    public String list(@ModelAttribute("search") AssetProductSearchDTO search, Model model) {
        normalize(search);
        model.addAttribute("productType", "ASSET");
        model.addAttribute("pagePath", "/asset-products");
        model.addAttribute("pageLabel", "ASSET PRODUCT");
        model.addAttribute("pageTitle", "자산형성상품");
        model.addAttribute("pageDescription", "목돈 마련과 자산 형성을 지원하는 상품을 확인할 수 있습니다.");
        model.addAttribute("ageOptions", java.util.List.of("청년층", "중장년층"));
        model.addAttribute("incomeOptions", java.util.List.of("소득기준 있음", "소득기준 없음"));
        try {
            var page = service.findAll(search);
            model.addAttribute("productPage", page);
            model.addAttribute("products", page.getProducts());
            model.addAttribute("loadError", false);
        } catch (Exception e) {
            log.error("자산형성 상품 목록 조회 실패", e);
            model.addAttribute("products", java.util.List.of());
            model.addAttribute("loadError", true);
        }
        return "policy/asset-list";
    }

    @ResponseBody
    @GetMapping("/api/asset-products/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void normalize(AssetProductSearchDTO search) {
        search.setPage(Math.max(0, search.getPage()));
        if (search.getSize() < 10 || search.getSize() > 50 || search.getSize() % 10 != 0) search.setSize(20);
    }
}
