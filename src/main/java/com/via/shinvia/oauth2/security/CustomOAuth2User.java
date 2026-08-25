package com.via.shinvia.oauth2.security;

import com.via.shinvia.oauth2.domain.OAuth2LoginStatus;
import com.via.shinvia.oauth2.domain.SocialProvider;
import com.via.shinvia.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter @Builder @AllArgsConstructor
public class CustomOAuth2User implements OAuth2User{

    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final SocialProvider provider;
    private final String providerUserId;
    private final String providerEmail;
    private final OAuth2LoginStatus loginStatus;
    private final User user;

    @Override
    public String getName() {
        return provider.name() + ":" + providerUserId;
    }

    public Long getUserId() {
        return user.getUserId();
    }
}
