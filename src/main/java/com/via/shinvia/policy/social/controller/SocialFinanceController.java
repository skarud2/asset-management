package com.via.shinvia.policy.social.controller;

import com.via.shinvia.policy.social.dto.SocialFinanceSearchDTO;
import com.via.shinvia.policy.social.service.SocialFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
// 사회연대금융 화면 및 상세조회 기능
public class SocialFinanceController {
    private final SocialFinanceService service;

    @GetMapping("/social-finance")
    public String list(@ModelAttribute("search") SocialFinanceSearchDTO search, Model model) {
        normalize(search);
        model.addAttribute("productType", "SOCIAL");
        model.addAttribute("pagePath", "/social-finance");
        model.addAttribute("pageLabel", "SOCIAL FINANCE");
        model.addAttribute("pageTitle", "사회연대금융");
        model.addAttribute("pageDescription", "사회적경제기업과 금융취약계층을 위한 금융상품을 확인할 수 있습니다.");
        model.addAttribute("ageOptions", java.util.List.of());
        model.addAttribute("incomeOptions", java.util.List.of());
        try {
            var page = service.findAll(search);
            model.addAttribute("productPage", page);
            model.addAttribute("products", page.getProducts());
            model.addAttribute("loadError", false);
        } catch (Exception e) {
            log.error("사회연대금융 상품 목록 조회 실패", e);
            model.addAttribute("products", java.util.List.of());
            model.addAttribute("loadError", true);
        }
        return "policy/social-list";
    }

    @ResponseBody
    @GetMapping("/api/social-finance/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void normalize(SocialFinanceSearchDTO search) {
        search.setPage(Math.max(0, search.getPage()));
        if (search.getSize() < 10 || search.getSize() > 50 || search.getSize() % 10 != 0) search.setSize(20);
    }
}
