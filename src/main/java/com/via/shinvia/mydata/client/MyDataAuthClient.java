package com.via.shinvia.mydata.client;

import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Slf4j
@Component
public class MyDataAuthClient {


    private final RestClient restClient;
    private final MyDataProperties myDataProperties;

    public MyDataAuthClient(@Value("${shinvia-client.mock.url:http://localhost:9090}") String mockServerUrl,
                            MyDataProperties myDataProperties) {
        this.myDataProperties = myDataProperties;

        // 목 서버로 전송하는 HTTP 요청 및 응답 로깅 인터셉터
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            log.info("\n======================= [HTTP OUTGOING REQUEST TO MOCK SERVER] =======================");
            log.info("Request URI    : {}", request.getURI());
            log.info("Request Method : {}", request.getMethod());
            log.info("Request Headers: {}", request.getHeaders());
            if (body != null && body.length > 0) {
                log.info("Request Body   : {}", new String(body, StandardCharsets.UTF_8));
            }
            log.info("=====================================================================================");

            ClientHttpResponse response = execution.execute(request, body);

            log.info("\n====================== [HTTP INCOMING RESPONSE FROM MOCK SERVER] =====================");
            log.info("Status Code    : {}", response.getStatusCode());
            log.info("Response Headers: {}", response.getHeaders());
            log.info("=====================================================================================\n");
            return response;
        };

        this.restClient = RestClient.builder()
                .baseUrl(mockServerUrl)
                .requestInterceptor(loggingInterceptor)
                .build();
    }

   /* public MyDataAuthClient(RestClient restClient, MyDataProperties myDataProperties) {

        this.restClient = restClient;
        this.myDataProperties = myDataProperties;
    }*/


    // 1. 인가 코드 발급 요청 (GET /v2/oauth/2.0/authorize)
    public String requestAuthorize(String userCi) {
        String tranId = generateTranId();
        // state 파라미터에 userCi를 담아서 전송 (목 서버 변경 없이 콜백 시 userCi 복원 가능)
        String state = userCi != null ? userCi : "1";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/v2/oauth/2.0/authorize")
                .queryParam("org_code", myDataProperties.getOrgCode())
                .queryParam("response_type", "code")
                .queryParam("client_id", myDataProperties.getClientId())
                .queryParam("redirect_uri", myDataProperties.getRedirectUri())
                .queryParam("app_scheme", myDataProperties.getAppScheme())
                .queryParam("state", state);

        log.info("[MyData Client] 인가코드 요청 - userCi: {}, orgCode: {}, tranId: {}", userCi, myDataProperties.getOrgCode(), tranId);

        ResponseEntity<Void> response = restClient.get()
                .uri(uriBuilder.build().toUriString())
                .header("x-api-tran-id", tranId)
                .header("x-user-ci",userCi)
                .retrieve()
                .toBodilessEntity();

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        log.info("[MyData Client] 인가코드 발급 완료 - Location: {}", location);
        return location;
    }

    private String generateState() {
        return "MOCK_TRAN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }


    // 2. 접근 토큰 발급 요청 (POST /v2/oauth/2.0/token)
    public MyDataAuthTokenResponseDto requestAccessToken(String code) {
        String tranId = generateTranId();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("org_code", myDataProperties.getOrgCode());
        formData.add("grant_type", "authorization_code");
        if (code != null) formData.add("code", code);
        formData.add("client_id", myDataProperties.getClientId());
        if (myDataProperties.getClientSecret() != null) formData.add("client_secret", myDataProperties.getClientSecret());
        formData.add("redirect_uri", myDataProperties.getRedirectUri());

        log.info("[MyData Client] Access Token 발급 요청 - code: {}, tranId: {}", code, tranId);

        MyDataAuthTokenResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataAuthTokenResponseDto.class);

        log.info("[MyData Client] Access Token 발급 완료 - accessToken: {}", response != null ? response.getAccessToken() : null);
        return response;
    }


     // 3. 접근 토큰 갱신 요청 (POST /v2/oauth/2.0/token - refresh_token)
    public MyDataAuthTokenResponseDto refreshAccessToken(String refreshToken) {
        String tranId = generateTranId();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", myDataProperties.getClientId());
        formData.add("client_secret", myDataProperties.getClientSecret());
        formData.add("org_code", myDataProperties.getOrgCode());
        formData.add("redirect_uri",myDataProperties.getRedirectUri());
        formData.add("is_refreshed", "N");
        log.info("{}",formData.toString());
        log.info("[MyData Client] Access Token 갱신 요청 - refreshToken: {}, tranId: {}", refreshToken, tranId);

        MyDataAuthTokenResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataAuthTokenResponseDto.class);

        log.info("[MyData Client] Access Token 갱신 완료 - newAccessToken: {}", response != null ? response.getAccessToken() : null);
        return response;
    }

    //4. 토큰 폐기 요청 (POST /v2/oauth/2.0/revoke)

    public MyDataCommonResponseDto revokeToken(String token, String revokeType) {
        String tranId = generateTranId();

        String effectiveClientId = myDataProperties.getClientId();
        String effectiveClientSecret = myDataProperties.getClientSecret();
        String effectiveOrgCode = myDataProperties.getOrgCode();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (token != null) formData.add("token", token);
        formData.add("client_id", effectiveClientId);
        if (effectiveClientSecret != null) formData.add("client_secret", effectiveClientSecret);
        formData.add("org_code", effectiveOrgCode);
        formData.add("revoke_type", revokeType != null ? revokeType : "0");

        log.info("[MyData Client] 토큰 폐기 요청 - token: {}, orgCode: {}, tranId: {}", token, effectiveOrgCode, tranId);

        MyDataCommonResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataCommonResponseDto.class);

        log.info("[MyData Client] 토큰 폐기 완료 - response: {}", response);
        return response;
    }

    public String generateTranId() {
        return "MOCK_TRAN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
