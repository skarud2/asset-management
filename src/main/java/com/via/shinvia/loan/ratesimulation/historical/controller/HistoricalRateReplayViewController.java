package com.via.shinvia.loan.ratesimulation.historical.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 과거 금리인상기 재현 API를 브라우저에서 수동 테스트하기 위한 화면 (로컬 테스트 전용)
@Controller
public class HistoricalRateReplayViewController {

    @GetMapping("/loans/{loanId}/historical-rate-replay")
    public String historicalRateReplayPage(@PathVariable Long loanId, Model model) {
        model.addAttribute("loanId", loanId);
        return "loan/historical-rate-replay";
    }
}
