package com.via.shinvia.policy.welfare.controller;

import com.via.shinvia.policy.welfare.dto.WelfareSupportSearchDTO;
import com.via.shinvia.policy.welfare.service.WelfareSupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
// 복합지원 화면 및 상세조회 기능
public class WelfareSupportController {
    private final WelfareSupportService service;

    @GetMapping("/welfare-support")
    public String list(@ModelAttribute("search") WelfareSupportSearchDTO search, Model model) {
        normalize(search);
        model.addAttribute("productType", "WELFARE");
        model.addAttribute("pagePath", "/welfare-support");
        model.addAttribute("pageLabel", "WELFARE SUPPORT");
        model.addAttribute("pageTitle", "복합지원");
        model.addAttribute("pageDescription", "금융·고용·복지 서비스를 연계한 지원 정보를 확인할 수 있습니다.");
        model.addAttribute("ageOptions", java.util.List.of("영유아", "아동", "청소년", "아동·청소년", "청년·성인", "성인"));
        model.addAttribute("incomeOptions", java.util.List.of("저소득층·취약계층"));
        try {
            var page = service.findAll(search);
            model.addAttribute("productPage", page);
            model.addAttribute("products", page.getProducts());
            model.addAttribute("loadError", false);
        } catch (Exception e) {
            log.error("복합지원 상품 목록 조회 실패", e);
            model.addAttribute("products", java.util.List.of());
            model.addAttribute("loadError", true);
        }
        return "policy/welfare-list";
    }

    @ResponseBody
    @GetMapping("/api/welfare-support/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void normalize(WelfareSupportSearchDTO search) {
        search.setPage(Math.max(0, search.getPage()));
        if (search.getSize() < 10 || search.getSize() > 50 || search.getSize() % 10 != 0) search.setSize(20);
    }
}
