package com.via.shinvia.oauth2.controller;

import com.via.shinvia.oauth2.domain.PendingSocialUser;
import com.via.shinvia.oauth2.dto.SocialSignupRequestDto;
import com.via.shinvia.oauth2.service.SocialSignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/social/signup")
@RequiredArgsConstructor
public class SocialSignupController {
    private static final String PENDING_SOCIAL_USER="PENDING_SOCIAL_USER";
    private final SocialSignupService socialSignupService;

    @GetMapping
    public String signupForm(HttpSession session, Model model){
        PendingSocialUser pendingSocialUser = getPendingSocialUser(session);
        if (pendingSocialUser == null) {
            return "redirect:/login?socialError";
        }

        if(!model.containsAttribute("socialSignupRequest")) {
            SocialSignupRequestDto request = new SocialSignupRequestDto();
            model.addAttribute("socialSignupRequest", request);
        }

        model.addAttribute("providerEmail", pendingSocialUser.providerEmail());

        return "user/social-signup";
    }

    @PostMapping
    public String signup(@Valid @ModelAttribute("socialSignupRequest") SocialSignupRequestDto request,
                         BindingResult bindingResult,
                         HttpServletRequest httpRequest,
                         HttpServletResponse httpResponse,
                         Model model) {
        HttpSession session = httpRequest.getSession(false);
        PendingSocialUser pendingSocialUser = getPendingSocialUser(session);

        if(pendingSocialUser == null) {
            return "redirect:/login?socialError";
        }

        //검증 실패시 화면에 이메일 다시 표시하도록 렌더링에 필요한 값 넘김
        if(bindingResult.hasErrors()) {
            model.addAttribute("providerEmail", pendingSocialUser.providerEmail());
            return "user/social-signup";
        }

        try { socialSignupService.signup(request, pendingSocialUser);}
        catch (RuntimeException exception) {
            model.addAttribute("signupError", exception.getMessage());
            model.addAttribute("providerEmail", pendingSocialUser.providerEmail());
            return "user/social-signup";
        }

        clearAuthentication(httpRequest, httpResponse);

        return "redirect:/login?signup";
    }

    private PendingSocialUser getPendingSocialUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(PENDING_SOCIAL_USER);

        if(value instanceof PendingSocialUser pendingSocialUser){
            return pendingSocialUser;
        }
        return null;
    }

    private void clearAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
