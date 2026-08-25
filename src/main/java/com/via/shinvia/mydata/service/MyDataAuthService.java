package com.via.shinvia.mydata.service;

import com.via.shinvia.client.card.mapper.CardMapper;
import com.via.shinvia.mydata.client.MyDataAuthClient;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

//import com.via.shinvia.mapper.card.CardMapper;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 마이데이터 OAuth 연동 비즈니스 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyDataAuthService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(365);

    private final MyDataAuthClient myDataAuthClient;
    private final StringRedisTemplate redisTemplate;
    private final CardMapper cardMapper;
    private final LocalValidatorFactoryBean localValidatorFactoryBean;

    /**
     * 1. 인가코드 발급 URL 요청
     */
    public String getAuthorizeUrl(String userCi) {
        return myDataAuthClient.requestAuthorize(userCi);
    }

    /**
     * 2. 인가코드로 Access Token 및 Refresh Token 발급 및 Redis 저장
     */
    public MyDataAuthTokenResponseDto issueTokens(String userCi, String code) {
        MyDataAuthTokenResponseDto response = myDataAuthClient.requestAccessToken(code);

        if (response != null) {
            saveTokensToRedis(userCi, response.getAccessToken(), response.getRefreshToken());
        }

        return response;
    }

    /**
     * 3. Refresh Token으로 Access Token 갱신 및 Redis 저장
     */
    public MyDataAuthTokenResponseDto refreshToken(String refreshToken) {
        String cleanToken = cleanToken(refreshToken);
        if (!StringUtils.hasText(cleanToken)) {
            throw new IllegalArgumentException("refreshToken이 비어있습니다.");
        }

        log.info("[Redis Token Refresh] 입력된 cleanToken: '{}'", cleanToken);

        // 2. Redis에서 토큰(cleanToken) 자체를 키로 userCi 추출 시도
        String userCi = redisTemplate.opsForValue().get(cleanToken);
        if (!StringUtils.hasText(userCi)) {
            userCi = redisTemplate.opsForValue().get("mydata:rt:ci:" + cleanToken);
        }
        if (!StringUtils.hasText(userCi)) {
            userCi = redisTemplate.opsForValue().get("mydata:at:ci:" + cleanToken);
        }

        // 3. Redis 조회가 안 된 경우 토큰 규격(mock_rt_{ci}_{uuid})에서 userCi 파싱 시도
        if (!StringUtils.hasText(userCi)) {
            userCi = extractCiFromToken(cleanToken);
        }

        log.info("[Redis Token Refresh] 토큰 갱신 진행 - userCi: {}, cleanToken: {}", userCi, cleanToken);

        // 5. 목 서버로 토큰 갱신 요청
        log.info("cleanToken :" + cleanToken);
        MyDataAuthTokenResponseDto response = myDataAuthClient.refreshAccessToken(cleanToken);

        // 6. 획득한 userCi와 신규 발급된 토큰 정보로 Redis 갱신
        if (response != null) {
            saveTokensToRedis(userCi, response.getAccessToken(), response.getRefreshToken());
        }

        return response;
    }

    private String extractCiFromToken(String token) {
        if (token == null) return null;
        try {
            String[] parts = token.split("_");
            if (parts.length >= 4) {
                // 예: mock_rt_5_uuid -> parts[2] = "5"
                return parts[2];
            } else if (parts.length == 3) {
                return parts[1];
            }
        } catch (Exception e) {
            log.warn("[Token Extract Warning] 토큰 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 4. Access Token 폐기 (Redis의 ci:at 및 at:ci 2개 키 삭제)
     */
    public MyDataCommonResponseDto revokeToken(String token, String revokeType) {
        String cleanToken = cleanToken(token);
        if (!StringUtils.hasText(cleanToken)) {
            throw new IllegalArgumentException("폐기할 token이 비어있습니다.");
        }

        // 1. 토큰 폐기 전 Redis에서 userCi 사전 조회 (접두어 없는 cleanToken 우선)
        String userCi = redisTemplate.opsForValue().get(cleanToken);
        if (!StringUtils.hasText(userCi)) {
            userCi = redisTemplate.opsForValue().get("mydata:at:ci:" + cleanToken);
        }
        if (!StringUtils.hasText(userCi)) {
            userCi = redisTemplate.opsForValue().get("mydata:rt:ci:" + cleanToken);
        }
        if (!StringUtils.hasText(userCi)) {
            userCi = extractCiFromToken(cleanToken);
        }

        String currentAt = null;
        if (StringUtils.hasText(userCi)) {
            currentAt = redisTemplate.opsForValue().get("mydata:ci:at:" + userCi);
        }

        log.info("[Redis Token Revoke] 폐기 요청 수신 - token: '{}', userCi: '{}', currentAt: '{}'", cleanToken, userCi, currentAt);

        // 2. 외부 목 서버로 토큰 폐기 요청
        MyDataCommonResponseDto response = myDataAuthClient.revokeToken(cleanToken, revokeType);

        // 3. Redis에서 입력받은 토큰 키 삭제
        redisTemplate.delete(cleanToken);
        redisTemplate.delete("mydata:at:ci:" + cleanToken);
        redisTemplate.delete("mydata:rt:ci:" + cleanToken);

        if (StringUtils.hasText(currentAt)) {
            redisTemplate.delete("mydata:at:ci:" + currentAt);
        }

        if (StringUtils.hasText(userCi)) {
            redisTemplate.delete("mydata:ci:at:" + userCi);
            redisTemplate.delete("at:" + userCi); // 목 서버가 생성한 at:{ci} 키도 함께 삭제!
            log.info("[Redis Token Revoke] CI[{}]의 Access Token 키 (mydata:ci:at:{}, at:{}) 삭제 완료", userCi, userCi, userCi);
        }

        return response;
    }

    /**
     * Redis 양방향 키-값 매핑 저장 (TTL: AccessToken=1시간, RefreshToken=1년)
     * 1) ci : accesstoken (mydata:ci:at:{ci} -> accessToken)
     * 2) ci : refreshtoken (mydata:ci:rt:{ci} -> refreshToken)
     * 3) accesstoken : ci (mydata:at:ci:{accessToken} -> ci)
     * 4) refreshtoken : ci (mydata:rt:ci:{refreshToken} -> ci)
     */
    public void saveTokensToRedis(String userCi, String accessToken, String refreshToken) {
        if (!StringUtils.hasText(userCi)) {
            log.warn("[Redis Token Storage] userCi가 없어 Redis에 저장하지 않습니다.");
            return;
        }

        String cleanAt = cleanToken(accessToken);
        String cleanRt = cleanToken(refreshToken);

        // 1. 기존 Access Token & Refresh Token 역방향 키 삭제 (token:ci Key 2개)
        String oldAccessToken = redisTemplate.opsForValue().get("mydata:ci:at:" + userCi);
        if (StringUtils.hasText(oldAccessToken)) {
            redisTemplate.delete(oldAccessToken);
            redisTemplate.delete("mydata:at:ci:" + oldAccessToken);
        }

        String oldRefreshToken = redisTemplate.opsForValue().get("mydata:ci:rt:" + userCi);
        if (StringUtils.hasText(oldRefreshToken)) {
            redisTemplate.delete(oldRefreshToken);
            redisTemplate.delete("mydata:rt:ci:" + oldRefreshToken);
        }

        // 2. 신규 Access Token 저장 (Key: token 자체, Value: userCi)
        if (StringUtils.hasText(cleanAt)) {
            redisTemplate.opsForValue().set("mydata:ci:at:" + userCi, cleanAt, ACCESS_TOKEN_TTL);
            redisTemplate.opsForValue().set("mydata:at:ci:" + cleanAt, userCi, ACCESS_TOKEN_TTL);
        }

        // 3. 신규 Refresh Token 저장 (Key: token 자체, Value: userCi)
        if (StringUtils.hasText(cleanRt)) {
            redisTemplate.opsForValue().set("mydata:ci:rt:" + userCi, cleanRt, REFRESH_TOKEN_TTL);
            redisTemplate.opsForValue().set("mydata:rt:ci:" + cleanRt, userCi, REFRESH_TOKEN_TTL);
        }

        log.info("[Redis Token Storage] CI[{}] 토큰 Key-Value 갱신 완료 (Access TTL: 1시간, Refresh TTL: 365일)", userCi);
    }

    /**
     * 토큰에서 앞뒤 따옴표(', "), Bearer 접두어 및 공백을 완벽하게 제거
     */
    private String cleanToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        String cleaned = token.trim();
        while ((cleaned.startsWith("'") && cleaned.endsWith("'") && cleaned.length() > 1) ||
               (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1)) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith("Bearer ") || cleaned.startsWith("bearer ")) {
            cleaned = cleaned.substring(7).trim();
        }
        while ((cleaned.startsWith("'") && cleaned.endsWith("'") && cleaned.length() > 1) ||
               (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1)) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    public String getAccessToken(Long connectionId) {
        String accessToken = redisTemplate.opsForValue().get(
                "mydata:ci:at:" + connectionId
        );
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException(
                    "마이데이터 Access Token이 없거나 만료되었습니다."
            );
        }
        return accessToken;
    }

    public Long getAccessTokenTtl(Long connectionId) {
        if (connectionId == null) {
            return 0L;
        }

        // 1. mydata:ci:at:{userId} 키의 TTL 조회
        String accessTokenKey = "mydata:ci:at:" + connectionId;
        Long remainingSeconds = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);

        if (remainingSeconds == null || remainingSeconds < 0) {
            return 0L;
        }

        return remainingSeconds;
    }

    public void refreshAccessToken(Long connectionId) {

        String userCi = String.valueOf(connectionId);

        String refreshToken = redisTemplate.opsForValue().get("mydata:ci:rt:" + userCi);

        if (!StringUtils.hasText(refreshToken)) {
            log.warn(
                    "[MyData Token Refresh] Refresh Token 없음 - connectionId: {}",
                    connectionId
            );
            return;
        }

        MyDataAuthTokenResponseDto response = myDataAuthClient.refreshAccessToken(refreshToken);

        if (response == null || !StringUtils.hasText(response.getAccessToken())) {
            throw new IllegalStateException("마이데이터 Access Token 재발급 실패");
        }

        String newRefreshToken = StringUtils.hasText(response.getRefreshToken())
                        ? response.getRefreshToken() : refreshToken;

        saveTokensToRedis(
                userCi,
                response.getAccessToken(),
                newRefreshToken
        );
    }
}

