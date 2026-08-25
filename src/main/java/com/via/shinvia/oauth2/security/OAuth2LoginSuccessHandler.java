package com.via.shinvia.oauth2.security;

import com.via.shinvia.mydata.service.MyDataLoginService;
import com.via.shinvia.oauth2.domain.OAuth2LoginStatus;
import com.via.shinvia.oauth2.domain.PendingSocialUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.via.shinvia.login.security.LoginSuccessHandler.SESSION_EXTENSION_DEADLINE;
import static com.via.shinvia.login.security.LoginSuccessHandler.SESSION_EXTENSION_DISPLAY_MILLIS;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    public static final String PENDING_SOCIAL_USER = "PENDING_SOCIAL_USER";
    private final MyDataLoginService myDataLoginService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication
    ) throws IOException, ServletException {
        if(!(authentication.getPrincipal() instanceof CustomOAuth2User customOAuth2User)) {
            throw new IllegalStateException("OAuth2 인증 사용자 정보를 확인할 수 없습니다.");
        }

        OAuth2LoginStatus loginStatus = customOAuth2User.getLoginStatus();

        switch (loginStatus) {
            case EXISTING_USER -> handleExistingUser(request, response, customOAuth2User);
            case LINK_REQUIRED -> handlePendingUser(request, response, customOAuth2User,"/social/link");
            case NEW_USER -> handlePendingUser(request, response, customOAuth2User,"/social/signup");
        }
    }

    private void handleExistingUser(HttpServletRequest request, HttpServletResponse response, CustomOAuth2User customOAuth2User) throws IOException {
        HttpSession session = request.getSession(false);
        if(session!=null){
            session.removeAttribute(PENDING_SOCIAL_USER);
            session.setMaxInactiveInterval(3600);
            session.setAttribute(
                    SESSION_EXTENSION_DEADLINE,
                    System.currentTimeMillis() + SESSION_EXTENSION_DISPLAY_MILLIS
            );
        }

        myDataLoginService.refreshTokenOnLogin(
                customOAuth2User.getUserId()
        );

        response.sendRedirect("/");
    }

    private void handlePendingUser(HttpServletRequest request, HttpServletResponse response,
                                   CustomOAuth2User customOAuth2User, String redirectUrl) throws IOException {
        PendingSocialUser pendingSocialUser= new PendingSocialUser(customOAuth2User.getProvider(),
                                                                    customOAuth2User.getProviderUserId(),
                                                                    customOAuth2User.getProviderEmail());
        request.getSession().setAttribute(PENDING_SOCIAL_USER, pendingSocialUser);
        response.sendRedirect(redirectUrl);
    }
}
