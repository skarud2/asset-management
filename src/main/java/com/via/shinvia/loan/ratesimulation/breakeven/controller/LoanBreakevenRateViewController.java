package com.via.shinvia.loan.ratesimulation.breakeven.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 한계금리 역산 API를 브라우저에서 수동 테스트하기 위한 화면 (로컬 테스트 전용)
@Controller
public class LoanBreakevenRateViewController {

    @GetMapping("/loans/breakeven-rate-test")
    public String breakevenRateTestPage() {
        return "loan/breakeven-rate-test";
    }
}
