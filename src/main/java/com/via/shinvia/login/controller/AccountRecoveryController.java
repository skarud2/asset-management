package com.via.shinvia.login.controller;

import com.via.shinvia.login.dto.FindLoginEmailRequestDto;
import com.via.shinvia.login.dto.FindLoginEmailResponseDto;
import com.via.shinvia.login.dto.ResetPasswordRequestDto;
import com.via.shinvia.login.service.AccountRecoveryService;
import com.via.shinvia.user.service.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/find")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    @GetMapping("/id")
    public String findIdForm(Model model) {
        model.addAttribute("findLoginEmailRequest", new FindLoginEmailRequestDto());

        return "user/find-id";
    }

    @PostMapping("/id")
    public String findId(
            @Valid @ModelAttribute("findLoginEmailRequest")
            FindLoginEmailRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "user/find-id";
        }

        FindLoginEmailResponseDto result = accountRecoveryService.findLoginEmail(request);

        model.addAttribute("result", result);
        model.addAttribute("searched", true);

        return "user/find-id";
    }

    @GetMapping("/pw")
    public String findPasswordPage() {
        return "user/find-pw";
    }

    @GetMapping("/pw/reset")
    public String resetPasswordPage(
            HttpSession session,
            Model model
    ) {
        String verifiedEmail =
                (String) session.getAttribute(EmailVerificationService.PASSWORD_RESET_VERIFIED_EMAIL_KEY);

        if (verifiedEmail == null) {
            return "redirect:/find/pw";
        }

        model.addAttribute("resetPasswordRequest", new ResetPasswordRequestDto());

        return "user/reset-pw";
    }

    @PostMapping("/pw/reset")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordRequest")
            ResetPasswordRequestDto request,
            BindingResult bindingResult,
            HttpSession session
    ) {
        String verifiedEmail = (String) session.getAttribute(EmailVerificationService.PASSWORD_RESET_VERIFIED_EMAIL_KEY);

        if (verifiedEmail == null) {
            return "redirect:/find/pw";
        }

        if (!Objects.equals(request.getPassword(), request.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "password.mismatch", "비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "user/reset-pw";
        }

        accountRecoveryService.resetPassword(verifiedEmail, request.getPassword());

        session.removeAttribute(EmailVerificationService.PASSWORD_RESET_VERIFIED_EMAIL_KEY);

        return "redirect:/login?reset";
    }
}