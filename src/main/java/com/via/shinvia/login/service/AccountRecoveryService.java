package com.via.shinvia.login.service;

import com.via.shinvia.login.dto.FindLoginEmailRequestDto;
import com.via.shinvia.login.dto.FindLoginEmailResponseDto;
import com.via.shinvia.oauth2.domain.SocialUser;
import com.via.shinvia.oauth2.mapper.SocialUserMapper;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserMapper userMapper;
    private final SocialUserMapper socialUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public FindLoginEmailResponseDto findLoginEmail(FindLoginEmailRequestDto request) {
        String userName = request.getUserName().trim();
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());

         User user = userMapper.findIdByNameAndPhone(userName, phoneNumber);

         if (user==null) {
             return null;
         }
        return toFindLoginEmailResponse(user);
    }

    private FindLoginEmailResponseDto toFindLoginEmailResponse(User user) {
        List<String> providers = socialUserMapper
                .findAllByUserId(user.getUserId())
                .stream()
                .map(SocialUser::getProvider)
                .map(String::valueOf)
                .toList();

        return FindLoginEmailResponseDto.builder()
                .maskedEmail(maskEmail(user.getLoginEmail()))
                .providers(providers)
                .build();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다.");
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        int visibleLength = Math.min(3, Math.max(1, localPart.length() / 2));

        return localPart.substring(0, visibleLength)
                + "***"
                + domainPart;
    }

    @Transactional
    public void resetPassword(String loginEmail, String newPassword) {

        String passwordHash = passwordEncoder.encode(newPassword);

        int updated = userMapper.updatePassword(loginEmail, passwordHash);

        if (updated != 1) {
            throw new IllegalArgumentException("비밀번호를 변경할 수 없는 계정입니다.");
        }
    }
}