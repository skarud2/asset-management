package com.via.shinvia.common;

import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private final CurrentUser currentUser;

    @ModelAttribute
    public void addLoginUser(
            Authentication authentication,
            Model model
    ) {
        Long userId = currentUser.getUserIdOrNull(authentication);

        if (userId == null) {
            return;
        }

        model.addAttribute(
                "loginUserName",
                currentUser.getUserName(authentication)
        );

        model.addAttribute(
                "loginUserEmail",
                currentUser.getLoginEmail(authentication)
        );
    }
}