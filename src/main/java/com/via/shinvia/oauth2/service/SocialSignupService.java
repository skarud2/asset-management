package com.via.shinvia.oauth2.service;

import com.via.shinvia.oauth2.domain.PendingSocialUser;
import com.via.shinvia.oauth2.domain.SocialUser;
import com.via.shinvia.oauth2.dto.SocialSignupRequestDto;
import com.via.shinvia.oauth2.mapper.SocialUserMapper;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.domain.UserRole;
import com.via.shinvia.user.domain.UserStatus;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SocialSignupService {
    private final UserMapper userMapper;
    private final SocialUserMapper socialUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SocialSignupRequestDto request, PendingSocialUser pendingSocialUser) {
        validatePendingSocialUser(pendingSocialUser);

        String loginEmail=pendingSocialUser.providerEmail().trim().toLowerCase(Locale.ROOT);
        validateDuplicateUser(loginEmail);
        validateDuplicateSocialUser(pendingSocialUser);

        User user = createUser(request, loginEmail);
        insertUser(user);

        SocialUser socialUser = createSocialUser(user.getUserId(), pendingSocialUser);
        insertSocialUser(socialUser);

        return user.getUserId();
    }




    private void validatePendingSocialUser(PendingSocialUser pendingSocialUser) {
        if (pendingSocialUser==null) {
            throw new IllegalStateException("소셜 로그인 정보가 없습니다. 다시 로그인해주세요.");
        }

        if(pendingSocialUser.provider() == null
                || !StringUtils.hasText(pendingSocialUser.providerUserId())
                || !StringUtils.hasText(pendingSocialUser.providerEmail())) {
            throw new IllegalStateException("소셜 로그인 정보가 올바르지 않습니다.");
        }
    }

    private void validateDuplicateUser(String loginEmail) {
        if (userMapper.existsByLoginEmail(loginEmail)) {
            throw new DuplicateKeyException("이미 가입된 이메일입니다.");
        }
    }

    private void validateDuplicateSocialUser(PendingSocialUser pendingSocialUser) {
        SocialUser existingSocialUser = socialUserMapper.findByProviderAndProviderUserId(
                pendingSocialUser.provider(), pendingSocialUser.providerUserId());
        if (existingSocialUser != null) {
            throw new DuplicateKeyException("이미 가입된 소셜 계정입니다.");
        }
    }

    private User createUser(SocialSignupRequestDto request, String loginEmail) {
        User user = new User();

        user.setLoginEmail(loginEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserName(request.getUserName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setBirthDate(request.getBirthDate());
        user.setUserStatus(UserStatus.ACTIVE);
        user.setUserRole(UserRole.USER);

        return user;
    }

    private SocialUser createSocialUser(Long userId, PendingSocialUser pendingSocialUser) {
        SocialUser socialUser = new SocialUser();

        socialUser.setUserId(userId);
        socialUser.setProvider(pendingSocialUser.provider());
        socialUser.setProviderUserId(pendingSocialUser.providerUserId());
        socialUser.setProviderEmail(pendingSocialUser.providerEmail().trim().toLowerCase(Locale.ROOT));

        return socialUser;
    }

    private void insertUser(User user) {
        int insertedCount = userMapper.insertUser(user);

        if(insertedCount != 1 || user.getUserId()==null) {
            throw new IllegalStateException("회원 정보 저장에 실패했습니다. ");
        }
    }

    private void insertSocialUser(SocialUser socialUser) {
        int insertedCount = socialUserMapper.insertSocialUser(socialUser);

        if(insertedCount != 1 || socialUser.getUserId()==null) {
            throw new IllegalStateException("회원 정보 저장에 실패했습니다. ");
        }
    }
}
