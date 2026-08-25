package com.via.shinvia.oauth2.service;

import com.via.shinvia.oauth2.domain.OAuth2LoginStatus;
import com.via.shinvia.oauth2.domain.SocialProvider;
import com.via.shinvia.oauth2.domain.SocialUser;
import com.via.shinvia.oauth2.mapper.SocialUserMapper;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.mapper.UserMapper;
import com.via.shinvia.oauth2.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserMapper userMapper;
    private final SocialUserMapper socialUserMapper;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        Object id = oauth2User.getAttribute("id");
        String providerUserId = String.valueOf(id);
        String providerEmail = extractKaKaoEmail(oauth2User);

        return determineLoginStatus(oauth2User, provider, providerUserId, providerEmail);
    }

    private String extractKaKaoEmail(OAuth2User oauth2User) {
        Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
        if (kakaoAccount == null) {
            throw new OAuth2AuthenticationException("카카오 계정 정보를 가져올 수 없습니다.");
        }

        Object email = kakaoAccount.get("email");
        if (email==null || email.toString().isBlank()) {
            throw new OAuth2AuthenticationException("이메일을 찾을 수 없습니다.");
        }
        return email.toString();
    }

    private OAuth2User determineLoginStatus(OAuth2User oauth2User, SocialProvider provider, String providerUserId, String providerEmail) {

        SocialUser socialUser = socialUserMapper.findByProviderAndProviderUserId(provider, providerUserId);
        if (socialUser != null) {
            User user = userMapper.findByUserId(socialUser.getUserId());
            if(user == null) {
                throw new OAuth2AuthenticationException("연결된 회원 정보를 찾을 수 없습니다.");
            }
            //기존에 이메일로 가입한 회원일 경우
            return new CustomOAuth2User(
                    oauth2User.getAuthorities(),
                    oauth2User.getAttributes(),
                    provider,
                    providerUserId,
                    providerEmail,
                    OAuth2LoginStatus.EXISTING_USER,
                    user);
        }

        //소셜로 처음 회원가입을 하는 경우
        User sameEmailUser = userMapper.findByLoginEmail(providerEmail);
        if(sameEmailUser != null) {
            return new CustomOAuth2User(
                    oauth2User.getAuthorities(),
                    oauth2User.getAttributes(),
                    provider,
                    providerUserId,
                    providerEmail,
                    OAuth2LoginStatus.LINK_REQUIRED,
                    null);
        }

        //새로 가입하는 회원일 경우
        return new CustomOAuth2User(
                oauth2User.getAuthorities(),
                oauth2User.getAttributes(),
                provider,
                providerUserId,
                providerEmail,
                OAuth2LoginStatus.NEW_USER,
                null);

    }

}
