package com.via.shinvia.user.controller;

import com.via.shinvia.login.dto.ResetPasswordRequestDto;
import com.via.shinvia.login.service.AccountRecoveryService;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.dto.UserProfileRequestDto;
import com.via.shinvia.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private static final String PROFILE_VERIFIED_USER_ID = "PROFILE_VERIFIED_USER_ID";
    private final CurrentUser currentUser;
    private final UserService userService;
    private final AccountRecoveryService accountRecoveryService;

    @GetMapping
    public String profile(
            Authentication authentication,
            HttpSession session,
            Model model
    ) {
        Long userId = currentUser.getUserId(authentication);
        Long verifiedUserId = (Long) session.getAttribute(PROFILE_VERIFIED_USER_ID);

        if (!userId.equals(verifiedUserId)) {
            return "user/profile-verify";
        }

        addProfile(userId, model);
        model.addAttribute("passwordForm", new ResetPasswordRequestDto());

        return "user/profile";
    }

    @PostMapping
    public String updateProfile(
            Authentication authentication,
            HttpSession session,
            @Valid @ModelAttribute("profile")
            UserProfileRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        Long userId = currentUser.getUserId(authentication);

        Long verifiedUserId = (Long) session.getAttribute(PROFILE_VERIFIED_USER_ID);

        if (!userId.equals(verifiedUserId)) {
            return "redirect:/profile";
        }

        if (bindingResult.hasErrors()) {

            User user = userService.findByUserId(userId);
            request.setLoginEmail(user.getLoginEmail());

            model.addAttribute("passwordForm", new ResetPasswordRequestDto());

            return "user/profile";
        }

        userService.updateProfile(userId, request);

        return "redirect:/profile?updated";
    }

    @PostMapping("/verify")
    public String verifyProfile(
            Authentication authentication,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        Long userId = currentUser.getUserId(authentication);

        if (!userService.matchesPassword(userId, password)) {
            model.addAttribute(
                    "passwordError",
                    "비밀번호가 일치하지 않습니다."
            );

            return "user/profile-verify";
        }

        session.setAttribute(
                PROFILE_VERIFIED_USER_ID,
                userId
        );

        return "redirect:/profile";
    }

    private void addProfile(
            Long userId,
            Model model
    ) {
        User user = userService.findByUserId(userId);

        UserProfileRequestDto profile =
                new UserProfileRequestDto();

        profile.setLoginEmail(user.getLoginEmail());
        profile.setUserName(user.getUserName());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setBirthDate(user.getBirthDate());

        model.addAttribute("profile", profile);
    }

    @PostMapping("/password")
    public String updatePassword(
            Authentication authentication,
            HttpSession session,
            @Valid @ModelAttribute("passwordForm")
            ResetPasswordRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        Long userId =
                currentUser.getUserId(authentication);

        Long verifiedUserId =
                (Long) session.getAttribute(
                        PROFILE_VERIFIED_USER_ID
                );

        if (!userId.equals(verifiedUserId)) {
            return "redirect:/profile";
        }

        if (!request.getPassword()
                .equals(request.getPasswordConfirm())) {

            bindingResult.rejectValue(
                    "passwordConfirm",
                    "password.mismatch",
                    "비밀번호가 일치하지 않습니다."
            );
        }

        if (bindingResult.hasErrors()) {

            addProfile(userId, model);

            return "user/profile";
        }

        User user =
                userService.findByUserId(userId);

        accountRecoveryService.resetPassword(
                user.getLoginEmail(),
                request.getPassword()
        );

        return "redirect:/profile?passwordUpdated";
    }
}
