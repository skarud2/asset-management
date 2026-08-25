package com.via.shinvia.oauth2.controller;

import com.via.shinvia.oauth2.domain.PendingSocialUser;
import com.via.shinvia.oauth2.dto.SocialLinkRequestDto;
import com.via.shinvia.oauth2.service.SocialLinkService;
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
@RequestMapping("/social/link")
@RequiredArgsConstructor
public class SocialLinkController {
    private static final String PENDING_SOCIAL_USER = "PENDING_SOCIAL_USER";

    private final SocialLinkService socialLinkService;

    @GetMapping
    public String linkForm(HttpSession session, Model model) {

        PendingSocialUser pendingSocialUser = getPendingSocialUser(session);

        if (pendingSocialUser == null) {
            return "redirect:/login?socialError";
        }

        if (!model.containsAttribute("socialLinkRequest")) {
            model.addAttribute(
                    "socialLinkRequest",
                    new SocialLinkRequestDto()
            );
        }

        model.addAttribute(
                "providerEmail",
                pendingSocialUser.providerEmail()
        );

        return "user/social-link";
    }

    @PostMapping
    public String link(
            @Valid @ModelAttribute("socialLinkRequest")
            SocialLinkRequestDto request,

            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {

        HttpSession session = httpRequest.getSession(false);
        PendingSocialUser pendingSocialUser = getPendingSocialUser(session);

        if (pendingSocialUser == null) {
            return "redirect:/login?socialError";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "providerEmail",
                    pendingSocialUser.providerEmail()
            );

            return "user/social-link";
        }

        try {
            socialLinkService.link(
                    pendingSocialUser,
                    request.getPassword()
            );
        } catch (RuntimeException exception) {

            model.addAttribute("linkError", exception.getMessage());
            model.addAttribute(
                    "providerEmail",
                    pendingSocialUser.providerEmail()
            );

            return "user/social-link";
        }

        clearAuthentication(httpRequest, httpResponse);

        return "redirect:/login?linked";
    }

    private PendingSocialUser getPendingSocialUser(HttpSession session) {

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(PENDING_SOCIAL_USER);

        if (value instanceof PendingSocialUser pendingSocialUser) {
            return pendingSocialUser;
        }

        return null;
    }

    private void clearAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        new SecurityContextLogoutHandler()
                .logout(request, response, authentication);
    }

}
