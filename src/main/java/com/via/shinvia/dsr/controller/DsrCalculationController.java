package com.via.shinvia.dsr.controller;

import com.via.shinvia.dsr.dto.request.DsrCalculationRequestDto;
import com.via.shinvia.dsr.dto.result.DsrCalculationResultDto;
import com.via.shinvia.dsr.dto.type.*;
import com.via.shinvia.dsr.service.DsrCalculationService;
import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileService;
import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import com.via.shinvia.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dsr")
@RequiredArgsConstructor
public class DsrCalculationController {
    private final DsrCalculationService dsrCalculationService;
    private final CurrentUser currentUser;
    private final FinancialProfileService financialProfileService;

    @GetMapping
    public String showDsrCalculationForm(
            Authentication authentication,
            Model model
    ) {
        DsrCalculationRequestDto request =
                new DsrCalculationRequestDto();
        Long userId = currentUser.getUserIdOrNull(authentication);

        if (userId != null) {
            FinancialProfile profile = financialProfileService.findFinancialProfileByUserId(userId);

            if (profile != null) {
                request.setAnnualIncome(profile.getAnnualIncome());
            }
        }

        model.addAttribute("dsrCalculationRequest", request);

        addSelectionOptions(model);

        return "dsr/dsr-calculation";
    }


    @PostMapping
    public String calculateDsr(
            Authentication authentication,
            @Valid @ModelAttribute("dsrCalculationRequest")
            DsrCalculationRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        addSelectionOptions(model);

        if (bindingResult.hasErrors()) {
            return "dsr/dsr-calculation";
        }

        Long userId = currentUser.getUserIdOrNull(authentication);

        try {
            DsrCalculationResultDto result = dsrCalculationService.calculate(userId, request);

            model.addAttribute("result", result);

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "dsr/dsr-calculation";
    }


    private void addSelectionOptions(Model model) {
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("repaymentTypes", RepaymentType.values());
        model.addAttribute("interestRateTypes", InterestRateType.values());
        model.addAttribute("propertyRegions", PropertyRegion.values());
        model.addAttribute("rentalPropertyRegions", RentalPropertyRegion.values());
        model.addAttribute("housingOwnershipTypes", HousingOwnershipType.values());
    }

}
