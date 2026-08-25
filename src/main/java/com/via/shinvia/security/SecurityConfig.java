package com.via.shinvia.security;

import com.via.shinvia.login.security.LoginSuccessHandler;
import com.via.shinvia.oauth2.security.OAuth2LoginSuccessHandler;
import com.via.shinvia.oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Postman API 테스트용 CSRF 제외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/loans/recommendations/**",
                                "/api/loan-analysis/**",
                                "/api/admin/loan-product-catalogs/**",
                                "/api/policy/recommendation/**",
                                "/api/mydata/loans/**",
                                "/api/lifecycle/survey/**",
                                "/api/lifecycle/scenarios/**",
                                "/api/policy/recommendation/**",
                                "/api/mydata/oauth/**", //일단 임시허용
                                "/api/auth/token/**", // 일단 임시허용
                                "/api/accounts/**", // 계좌 조회 및 동기화 API 임시 허용
                                "/api/surplus-funds/preferences/**",
                                "/api/welfare-support/sync/**",
                                "/api/surplus-funds/preferences/**",
                                "/api/lifecycle/scenarios/**",
                                "/api/lifecycle/survey/**",
                                "/api/admin/investment-products/etfs/**",
                                "/api/admin/investment-products/funds/**"
                        )
                )

                // CORS 허용 (Authorization 헤더 포함)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.addAllowedOriginPattern("*");
                    config.addAllowedMethod("*");
                    config.addAllowedHeader("*");
                    config.setAllowCredentials(true);
                    return config;
                }))

                // URL 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/admin/investment-products/etfs/**",
                                "/api/surplus-funds/preferences/**",
                                "/loans/recommendations",
                                "/loans/recommendations/**",
                                "/css/loan-recommendation.css",
                                "/js/loan-recommendation.js",
                                "/",
                                "/login",
                                "/signup/**",
                                "/social/signup/**",
                                "/social/link/**",
                                "/api/email-verify/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                // Card sync 경로 허용
                                "/api/cards/sync/**",
                                // 정부 규제 안내
                                "/financial-policy/**",
                                // 금융정책 화면
                                "/policy-support",
                                "/policy-support/**",
                                "/api/policy-support/**",
                                "/asset-products",
                                "/social-finance",
                                "/welfare-support",
                                "/api/asset-products/**",
                                "/api/social-finance/**",
                                "/api/welfare-support/**",
                                // 대출상품 카탈로그
                                "/api/admin/loan-product-catalogs/**",
                                "/api/loan-product-catalogs/**",

                                // 복지로 API 및 스웨거
                                "/api/bokjiro/**",
                                "/api/localbokjiro/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",

                                // 정적 리소스
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",

                                //dsr 계산
                                "/dsr","/dsr/**",

                                //id, pw 찾기
                                "/find/id/**","/find/pw/**",
                                //생애주기시나리오
                                "/api/lifecycle/survey/**"


                        )
                        .permitAll()

                        .requestMatchers(
                                "/policy/recommendation",
                                "/policy/recommendation/**",
                                "/api/policy/recommendation/**"
                        )
                        .authenticated()

                        // 나머지 요청은 로그인 필요
                        .anyRequest()
                        .authenticated()
                )

                // 폼 로그인 설정
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginEmail")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                    response.setCharacterEncoding("UTF-8");
                                    response.getWriter().write(
                                            "{\"status\":401,\"error\":\"Unauthorized\","
                                                    + "\"message\":\"로그인이 필요합니다.\"}"
                                    );
                                },
                                PathPatternRequestMatcher.pathPattern("/api/**")
                        )
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureUrl("/login?oauthError")
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
