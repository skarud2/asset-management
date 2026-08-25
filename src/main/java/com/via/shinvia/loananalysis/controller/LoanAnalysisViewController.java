package com.via.shinvia.loananalysis.controller;

import com.via.shinvia.loananalysis.service.LoanAnalysisService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// 대출분석 화면 Controller
@Controller
@RequiredArgsConstructor
public class LoanAnalysisViewController {

    private final LoanAnalysisService loanAnalysisService;
    private final CurrentUser currentUser;

    // 부채 상환순위 화면
    @GetMapping("/loan-analysis/debt-priority")
    public String debtPriorityPage() {

        return "loananalysis/debt-priority";
    }
    // 대출 대안 비교 화면
    @GetMapping("/loan-analysis/scenarios")
    public String loanScenarioPage(
            Authentication authentication,
            Model model
    ) {
        Long userId = currentUser.getUserId(authentication);
        model.addAttribute("loans", loanAnalysisService.getActiveLoans(userId));

        return "loananalysis/loan-scenario";
    }
}
