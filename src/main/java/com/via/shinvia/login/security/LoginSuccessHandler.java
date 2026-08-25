package com.via.shinvia.login.security;

import com.via.shinvia.mydata.service.MyDataLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    public static final String SESSION_EXTENSION_DEADLINE = "sessionExtensionDeadlineEpochMillis";
    public static final long SESSION_EXTENSION_DISPLAY_MILLIS = 3_599_000L;

    private final MyDataLoginService myDataLoginService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if(authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            myDataLoginService.refreshTokenOnLogin(userDetails.getUserId());
        }

        var session = request.getSession();
        session.setMaxInactiveInterval(3600);
        session.setAttribute(
                SESSION_EXTENSION_DEADLINE,
                System.currentTimeMillis() + SESSION_EXTENSION_DISPLAY_MILLIS
        );

        response.sendRedirect("/");
    }
}
