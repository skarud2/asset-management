package com.via.shinvia.loan.recommendation.controller;

import com.via.shinvia.loan.recommendation.dto.LoanRecommendationRequest;
import com.via.shinvia.loan.recommendation.dto.LoanRecommendationResult;
import com.via.shinvia.loan.recommendation.model.LoanPurpose;
import com.via.shinvia.loan.recommendation.model.RepaymentCalculationMethod;
import com.via.shinvia.loan.recommendation.service.LoanRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/loans/recommendations")
@RequiredArgsConstructor
public class LoanRecommendationController {

    /**
     * Redis 로그인 기능이 준비되기 전 로컬 기능 테스트용 사용자.
     * 팀 로그인 기능이 정상화되면 제거할 것.
     */
    private static final String LOCAL_TEST_EMAIL =
            "loan-test@shinvia.local";

    private final LoanRecommendationService service;

    @GetMapping
    public String page(
            Authentication authentication,
            Model model
    ) {
        String loginEmail = resolveLoginEmail(authentication);

        LoanRecommendationRequest request =
                new LoanRecommendationRequest();

        prepareCommonModel(
                loginEmail,
                request,
                model
        );

        return "loan/recommendation";
    }

    @PostMapping
    public String recommend(
            Authentication authentication,
            @ModelAttribute("request")
            LoanRecommendationRequest request,
            Model model
    ) {
        String loginEmail = resolveLoginEmail(authentication);

        prepareCommonModel(
                loginEmail,
                request,
                model
        );

        try {
            LoanRecommendationResult result =
                    service.recommend(
                            loginEmail,
                            request
                    );

            model.addAttribute("result", result);

        } catch (IllegalArgumentException exception) {
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "loan/recommendation";
    }

    private void prepareCommonModel(
            String loginEmail,
            LoanRecommendationRequest request,
            Model model
    ) {
        model.addAttribute("request", request);
        model.addAttribute(
                "profile",
                service.findProfile(loginEmail)
        );
        model.addAttribute(
                "loanPurposes",
                LoanPurpose.values()
        );
        model.addAttribute(
                "calculationMethods",
                RepaymentCalculationMethod.values()
        );
        model.addAttribute(
                "rateTypeOptions",
                service.findRateTypeOptions()
        );
        model.addAttribute(
                "repaymentTypeOptions",
                service.findRepaymentTypeOptions()
        );
        model.addAttribute(
                "collateralTypeOptions",
                service.findCollateralTypeOptions()
        );
    }

    /**
     * 실제 로그인 사용자가 있으면 실제 계정을 사용하고,
     * 인증이 없으면 로컬 테스트 계정을 사용한다.
     */
    private String resolveLoginEmail(
            Authentication authentication
    ) {
        if (isAuthenticated(authentication)) {
            return authentication.getName();
        }

        return LOCAL_TEST_EMAIL;
    }

    private boolean isAuthenticated(
            Authentication authentication
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                instanceof AnonymousAuthenticationToken);
    }
}