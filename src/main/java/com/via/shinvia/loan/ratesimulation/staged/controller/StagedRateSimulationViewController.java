package com.via.shinvia.loan.ratesimulation.staged.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 계단식(경로형) 금리 시나리오 API를 브라우저에서 수동 테스트하기 위한 화면 (로컬 테스트 전용)
@Controller
public class StagedRateSimulationViewController {

    @GetMapping("/loans/{loanId}/staged-rate-simulation")
    public String stagedRateSimulationPage(@PathVariable Long loanId, Model model) {
        model.addAttribute("loanId", loanId);
        return "loan/staged-rate-simulation";
    }
}
