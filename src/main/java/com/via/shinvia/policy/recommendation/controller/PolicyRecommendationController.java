package com.via.shinvia.policy.recommendation.controller;

import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy/recommendation")
@RequiredArgsConstructor
public class PolicyRecommendationController {

    private final FinancialProfileService financialProfileService;
    private final CurrentUser currentUser;

    // 맞춤 금융지원상품 설문
    @GetMapping
    public String recommendationForm(
            Authentication authentication,
            Model model
    ) {
        Long userId = currentUser.getUserId(authentication);
        FinancialProfile financialProfile =
                financialProfileService.findFinancialProfileByUserId(userId);

        model.addAttribute("financialProfile", financialProfile);
        model.addAttribute("hasFinancialProfile", financialProfile != null);

        return "policy/recommendation/recommendation-form";
    }


    // 추천 결과 화면
    @GetMapping("/result")
    public String recommendationResult() {

        return "policy/recommendation/recommendation-result";
    }
}
