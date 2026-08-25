package com.via.shinvia.login.controller;

import com.via.shinvia.mydata.dto.TokenStatusResponse;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.via.shinvia.login.security.LoginSuccessHandler.SESSION_EXTENSION_DEADLINE;
import static com.via.shinvia.login.security.LoginSuccessHandler.SESSION_EXTENSION_DISPLAY_MILLIS;

@Slf4j
@RestController
@RequestMapping("/api/auth/token")
@RequiredArgsConstructor
public class ExternalTokenApiController {

    private final MyDataAuthService tokenService;
    private final MyDataConnectionService myDataConnectionService;
    private final CurrentUser currentUser;

    // 1. 모든 화면에서 호출할 토큰 상태 확인 API
    @GetMapping("/status")
    public ResponseEntity<TokenStatusResponse> getTokenStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(new TokenStatusResponse(false, 0L));
        }

        Long userId = currentUser.getUserId(authentication);
        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        if (connectionId == null) {
            return ResponseEntity.ok(new TokenStatusResponse(false, 0L));
        }

        // Redis에서 남은 TTL(초) 조회
        Long remainingSeconds = tokenService.getAccessTokenTtl(connectionId);
        log.info("redisTokenTTl : " + remainingSeconds);

        boolean hasToken = remainingSeconds != null && remainingSeconds > 0;
        return ResponseEntity.ok(new TokenStatusResponse(hasToken, hasToken ? remainingSeconds : 0L));
    }

    @GetMapping("/extension-status")
    public ResponseEntity<TokenStatusResponse> getExtensionStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.ok(new TokenStatusResponse(false, 0L));
        }
        Object deadlineValue = session.getAttribute(SESSION_EXTENSION_DEADLINE);
        long deadline = deadlineValue instanceof Number number ? number.longValue() : 0L;
        long remainingSeconds = Math.max(0L, (deadline - System.currentTimeMillis() + 999L) / 1000L);
        return ResponseEntity.ok(new TokenStatusResponse(remainingSeconds > 0, remainingSeconds));
    }

    // 2. [시간 연장 / 토큰 재발급] 버튼 클릭 시 호출할 API
    @PostMapping("/extend")
    public ResponseEntity<String> extendToken( Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        Long userId = currentUser.getUserId(authentication);
        // 1) Redis의 외부 API Access Token 갱신 (Refresh Token 활용)
        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        if (connectionId == null) {
            return ResponseEntity.badRequest().body("마이데이터 연동 정보가 없습니다.");
        }

        try {
            tokenService.refreshAccessToken(connectionId);
        } catch (Exception e) {
            log.warn(
                    "[ExternalTokenApiController] 토큰 갱신 실패 " +
                            "(userId: {}, connectionId: {}): {}",
                    userId,
                    connectionId,
                    e.getMessage()
            );
        }

        // 2) 내 서비스의 JSESSIONID 세션 만료 시간도 같이 연장
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setMaxInactiveInterval(3600);
            session.setAttribute(
                    SESSION_EXTENSION_DEADLINE,
                    System.currentTimeMillis() + SESSION_EXTENSION_DISPLAY_MILLIS
            );
        }

        return ResponseEntity.ok("토큰 및 세션 시간이 성공적으로 연장되었습니다.");
    }
}
