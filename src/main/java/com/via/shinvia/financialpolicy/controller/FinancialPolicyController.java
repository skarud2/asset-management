package com.via.shinvia.financialpolicy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/financial-policy")
public class FinancialPolicyController {

    // 스트레스 DSR 안내
    @GetMapping("/stress-dsr")
    public String stressDsr() {
        return "financialpolicy/stress-dsr";
    }

    // 가계대출·DSR 규제 안내
    @GetMapping("/household-dsr")
    public String householdDsr() {
        return "financialpolicy/household-dsr";
    }

    // 주택담보대출 규제 안내
    @GetMapping("/mortgage-regulation")
    public String mortgageRegulation() {
        return "financialpolicy/mortgage-regulation";
    }

    // 전세대출 규제 안내
    @GetMapping("/jeonse-regulation")
    public String jeonseRegulation() {
        return "financialpolicy/jeonse-regulation";
    }

    // 정책대출 제도 안내
    @GetMapping("/policy-loan")
    public String policyLoan() {
        return "financialpolicy/policy-loan";
    }
}
