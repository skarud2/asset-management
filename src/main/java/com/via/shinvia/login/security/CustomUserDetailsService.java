package com.via.shinvia.login.security;

import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.domain.UserStatus;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String loginEmail) throws UsernameNotFoundException {
        User user = userMapper.findByLoginEmail(loginEmail);
        if (user==null){
            throw new UsernameNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        if(user.getUserStatus()== UserStatus.SUSPENDED || user.getUserStatus()== UserStatus.WITHDRAWN){
            throw new UsernameNotFoundException("사용 불가 계정입니다.");
        }

        return new CustomUserDetails(user);
    }
}
