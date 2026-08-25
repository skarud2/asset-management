package com.via.shinvia.mydata.controller;

import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 마이데이터 OAuth 2.0 연동 및 토큰 수신 처리 컨트롤러
 * (OAuth 2.0 표준 규격: 1. 인가코드 요청(302 Redirect) -> 2. 콜백 수신 및 토큰 교환)
 */
@Slf4j
@RestController
@RequestMapping("/api/mydata/oauth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MyDataAuthController {

    private final MyDataAuthService myDataAuthService;
    private final MyDataProperties myDataProperties;
    private final CurrentUser currentUser;
    private final MyDataConnectionService myDataConnectionService;
    /**
     * 1. 마이데이터 인가코드 허용 요청 (HTTP 302 Redirect)
     * 유저의 브라우저를 신한 목 서버의 인가 페이지로 리다이렉트시킵니다.
     * 예: GET http://localhost:8080/api/mydata/oauth/authorize
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(Authentication authentication)
            {
                Long userId = currentUser.getUserId(authentication);
                Long connectionId = myDataConnectionService.startConnection(userId);

        log.info("[MyData Controller] 인가코드 요청 시작 - userCi: {}", connectionId);

        // 신한 목 서버 302 Location URL 획득
        String mockAuthorizeUrl = myDataAuthService.getAuthorizeUrl(String.valueOf(connectionId));

        log.info("[MyData Controller] 목 서버 인가 URL로 리다이렉트(302): {}", mockAuthorizeUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(mockAuthorizeUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    /**
     * 2. 마이데이터 인가코드 콜백 수신 및 토큰 발급
     * 신한 목 서버가 인가코드(code)와 함께 리다이렉트해오는 콜백 엔드포인트
     */
    @GetMapping("/callback")
    public ResponseEntity<MyDataAuthTokenResponseDto> callback(
            @RequestHeader(value = "api_tran_id", required = false) String apiTranId,
            @RequestParam(value = "org_code", required = false) String orgCode,
            @RequestParam(value = "state") String state,
            @RequestParam("code") String code) {
        Long connectionId;

        try {
            connectionId=Long.valueOf(state);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("올바르지 않은 마이데이터 연동 정보입니다.");
        }

        try {
            MyDataAuthTokenResponseDto tokenResponse= myDataAuthService.issueTokens(String.valueOf(connectionId), code);
            myDataConnectionService.completeConnection(connectionId);

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create("/mydata/result"))
                    .build();

        } catch (Exception e) {
            myDataConnectionService.failConnection(connectionId);
            throw e;
        }
    }

    /**
     * 3. Access Token 갱신 요청
     */
    @PostMapping("/refresh")
    public ResponseEntity<MyDataAuthTokenResponseDto> refresh(
            @RequestParam("refreshToken") String refreshToken) {
        log.info("[MyData Controller] Access Token 갱신 요청 - refreshToken: {}", refreshToken);

        MyDataAuthTokenResponseDto tokenResponse = myDataAuthService.refreshToken(refreshToken);

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 4. 토큰 폐기 요청
     */
    @PostMapping("/revoke")
    public ResponseEntity<MyDataCommonResponseDto> revoke(
            @RequestParam("token") String token,
        @RequestParam("revoke_type") String revokeType)
    {
        log.info("[MyData Controller] 토큰 폐기 요청 - token: {}", token);
        MyDataCommonResponseDto response = myDataAuthService.revokeToken(token,revokeType);

        return ResponseEntity.ok(response);
    }
}
