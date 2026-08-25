package com.via.shinvia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * CardSyncController(/api/cards/sync/**) 전용 체인.
 * 로그인 세션·MyData 동의 플로우가 아직 없어 전 구간 permitAll로 열어둔다.
 * 인증 플로우가 생기면 이 파일만 수정하면 된다.
 */
@Configuration
public class CardSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain cardSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/cards/sync/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
                // 로그인 세션이 없어 CSRF 토큰을 발급/전달할 방법이 없으므로, 이 구간(permitAll)에 한해 비활성화
                //.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
