package com.via.shinvia.loan.ratesimulation.marketimplied.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 시장 내재 금리 시나리오 API를 브라우저에서 수동 테스트하기 위한 화면 (로컬 테스트 전용)
@Controller
public class MarketImpliedRateViewController {

    @GetMapping("/loans/{loanId}/market-implied-simulation")
    public String marketImpliedSimulationPage(@PathVariable Long loanId, Model model) {
        model.addAttribute("loanId", loanId);
        return "loan/market-implied-simulation";
    }
}
