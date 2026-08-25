package com.via.shinvia.oauth2.service;

import com.via.shinvia.oauth2.domain.PendingSocialUser;
import com.via.shinvia.oauth2.domain.SocialUser;
import com.via.shinvia.oauth2.mapper.SocialUserMapper;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SocialLinkService {
    private final UserMapper userMapper;
    private final SocialUserMapper socialUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void link(PendingSocialUser pendingSocialUser, String password) {

        validatePendingSocialUser(pendingSocialUser);

        String email = pendingSocialUser.providerEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userMapper.findByLoginEmail(email);

        if (user == null) {
            throw new IllegalStateException("기존 회원 정보를 찾을 수 없습니다.");
        }

        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new IllegalStateException(
                    "비밀번호로 가입된 계정이 아닙니다."
            );
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        SocialUser existingSocialUser =
                socialUserMapper.findByProviderAndProviderUserId(
                        pendingSocialUser.provider(),
                        pendingSocialUser.providerUserId()
                );

        if (existingSocialUser != null) {
            throw new IllegalStateException("이미 연결된 소셜 계정입니다.");
        }

        boolean alreadyLinkedProvider =
                socialUserMapper.findAllByUserId(user.getUserId())
                        .stream()
                        .anyMatch(socialUser ->
                                socialUser.getProvider() == pendingSocialUser.provider()
                        );

        if (alreadyLinkedProvider) {
            throw new IllegalStateException("이미 해당 소셜 로그인이 연결되어 있습니다.");
        }

        SocialUser socialUser = new SocialUser();
        socialUser.setUserId(user.getUserId());
        socialUser.setProvider(pendingSocialUser.provider());
        socialUser.setProviderUserId(pendingSocialUser.providerUserId());
        socialUser.setProviderEmail(email);

        int insertedCount = socialUserMapper.insertSocialUser(socialUser);

        if (insertedCount != 1 || socialUser.getSocialUserId() == null) {
            throw new IllegalStateException("소셜 계정 연결에 실패했습니다.");
        }
    }

    private void validatePendingSocialUser(PendingSocialUser pendingSocialUser) {

        if (pendingSocialUser == null) {
            throw new IllegalStateException(
                    "소셜 로그인 정보가 없습니다. 다시 로그인해주세요."
            );
        }

        if (pendingSocialUser.provider() == null
                || !StringUtils.hasText(pendingSocialUser.providerUserId())
                || !StringUtils.hasText(pendingSocialUser.providerEmail())) {

            throw new IllegalStateException("소셜 로그인 정보가 올바르지 않습니다.");
        }
    }
}
