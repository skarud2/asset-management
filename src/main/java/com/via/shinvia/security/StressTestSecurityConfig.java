package com.via.shinvia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PersonalStressTestController(/api/stress-test/**)와 수동 테스트용 화면 전용 체인.
 * 로그인한 사용자의 재무 데이터만 조회하도록 인증을 요구한다.
 */
@Configuration
public class StressTestSecurityConfig {

    @Bean
    @Order(3)
    public SecurityFilterChain stressTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/stress-test/**", "/stress-test/personal")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

        return http.build();
    }
}
