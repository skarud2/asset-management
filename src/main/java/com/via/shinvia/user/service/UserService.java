package com.via.shinvia.user.service;

import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.domain.UserRole;
import com.via.shinvia.user.domain.UserStatus;
import com.via.shinvia.user.dto.UserProfileRequestDto;
import com.via.shinvia.user.dto.UserSignupRequestDto;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(
            UserSignupRequestDto request,
            String verifiedEmail
    ) {
        User user = new User();
        user.setLoginEmail(verifiedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserName(request.getUserName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBirthDate(request.getBirthDate());
        user.setUserStatus(UserStatus.ACTIVE);
        user.setUserRole(UserRole.USER);

        try{
            int insertedCount = userMapper.insertUser(user);
            if(insertedCount != 1) {
                throw new IllegalStateException("회원 저장에 실패했습니다.");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.", e);
        }
        return user.getUserId();
    }

    @Transactional(readOnly = true)
    public User findByUserId(Long userId) {
        User user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        return user;
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileRequestDto request) {
        int updatedCount = userMapper.updateProfile(
                userId,
                request.getUserName(),
                request.getPhoneNumber(),
                request.getBirthDate()
        );

        if (updatedCount != 1) {
            throw new IllegalStateException("회원 정보 수정에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public boolean matchesPassword(Long userId, String password) {
        User user = findByUserId(userId);

        return passwordEncoder.matches(
                password,
                user.getPasswordHash()
        );
    }
}
