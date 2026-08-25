package com.via.shinvia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * loan/ratesimulation 관련 API(/api/loans/**)와 수동 테스트용 화면 전용 체인.
 * 로그인 세션·MyData 동의 플로우가 아직 없어 전 구간 permitAll로 열어둔다.
 * 인증 플로우가 생기면 이 파일만 수정하면 된다. (CardSecurityConfig와 동일한 이유)
 */
@Configuration
public class LoanRateSimulationSecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain loanRateSimulationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(
                        "/api/loans/**",
                        "/loans/breakeven-rate-test",
                        "/loans/*/staged-rate-simulation",
                        "/loans/*/historical-rate-replay",
                        "/loans/*/market-implied-simulation"
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
