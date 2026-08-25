package com.via.shinvia.lifecycle.survey.controller;

import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileService;
import com.via.shinvia.lifecycle.common.model.*;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LifecycleSurveyViewController {

    private final FinancialProfileService financialProfileService;
    private final CurrentUser currentUser;

    /**
     * 생애주기 설문 화면
     */
    @GetMapping("/lifecycle/survey")
    public String lifecycleSurvey(
            Authentication authentication,
            Model model
    ) {

        Long userId = currentUser.getUserId(authentication);
        FinancialProfile financialProfile =
                financialProfileService.findFinancialProfileByUserId(userId);

        model.addAttribute("currentHousingTypes", CurrentHousingType.values());
        model.addAttribute("industryCodes", IndustryCode.values());
        model.addAttribute("salaryGrowthScenarios", SalaryGrowthScenario.values());
        model.addAttribute("lifestyleLevels", LifestyleLevel.values());
        model.addAttribute("housingTypes", HousingType.values());
        model.addAttribute("repaymentTypes", LifecycleRepaymentType.values());
        model.addAttribute("repaymentActions", RepaymentAction.values());
        model.addAttribute("financialProfile", financialProfile);
        model.addAttribute("hasFinancialProfile", financialProfile != null);

        // templates/lifecycle/lifecycle-survey.html 반환
        return "lifecycle/lifecycle-survey";
    }
}
