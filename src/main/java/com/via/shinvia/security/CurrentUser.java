package com.via.shinvia.security;

import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.oauth2.security.CustomOAuth2User;
import com.via.shinvia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    public Long getUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("로그인 필요");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal instanceof CustomOAuth2User oAuth2User) {
            if (oAuth2User.getUser() == null) {
                throw new IllegalStateException("소셜 회원가입이 완료되지 않은 사용자입니다.");
            }
            return oAuth2User.getUserId();
        }

        throw new IllegalStateException(
                "지원하지 않는 인증 객체입니다: " + principal.getClass().getName()
        );
    }

    public Long getUserIdOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal instanceof CustomOAuth2User oAuth2User) {
            if (oAuth2User.getUser() == null) {
                return null;
            }

            return oAuth2User.getUserId();
        }

        return null;
    }

    public User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("로그인 필요");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }

        if (principal instanceof CustomOAuth2User oAuth2User) {
            return oAuth2User.getUser();
        }

        throw new IllegalStateException(
                "지원하지 않는 인증 객체입니다: "
                        + principal.getClass().getName()
        );
    }

    public String getUserName(Authentication authentication) {
        return getUser(authentication).getUserName();
    }

    public String getLoginEmail(Authentication authentication) {
        return getUser(authentication).getLoginEmail();
    }
}
