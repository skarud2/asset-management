package com.via.shinvia.futuresim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

// /futuresim/**, /api/future-goal/** 전용 체인. 로그인한 사용자의 재무 데이터만 조회하도록 인증을 요구한다.
// StressTestSecurityConfig(security/, @Order(3))와 같은 패턴이지만, 이번 작업 범위가 futuresim/로
// 한정돼 있어 security/ 대신 futuresim/config/에 둔다.
@Configuration
public class FutureSimSecurityConfig {

    @Bean
    @Order(4)
    public SecurityFilterChain futureSimSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/futuresim/**", "/api/future-goal/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }
}
