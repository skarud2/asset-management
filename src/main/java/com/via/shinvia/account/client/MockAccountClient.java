package com.via.shinvia.account.client;

import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionResponse;
import com.via.shinvia.mydata.client.MyDataAuthClient;
import com.via.shinvia.mydata.config.MyDataProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class MockAccountClient {
    private static final String SUCCESS_CODE = "00000";
    private final MyDataAuthClient mydata;
    private final RestClient restClient;
    private final StringRedisTemplate redistemplate;

    public MockAccountClient(
            MyDataAuthClient mydata,
            RestClient.Builder restClientBuilder,
            @Value("${mock.account.base-url:http://localhost:9090}") String baseUrl,
            StringRedisTemplate redistemplate
    ) {
        this.mydata = mydata;
        this.redistemplate = redistemplate;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    // 마이데이터 공통 헤더 (Authorization, x-api-tran-id, x-api-type) 100% 자동 세팅 헬퍼
    private RestClient.RequestHeadersSpec<?> applyHeaders(RestClient.RequestHeadersSpec<?> spec, String authorization) {
        String token = (authorization != null && !authorization.isBlank())
                ? authorization
                : "Bearer mock_access_token";

        return spec.header("Authorization", token)
                   .header("x-api-tran-id", mydata.generateTranId())
                   .header("x-api-type", "user");
    }

    public AccountListResponse getAccounts(String authorization, String orgCode, String nextPage, int limit) {
        try {
            var spec = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v2/bank/accounts")
                                .queryParam("org_code", orgCode)
                                .queryParam("search_timestamp", "0")
                                .queryParam("limit", limit);
                        if (hasText(nextPage)) {
                            builder.queryParam("next_page", nextPage);
                        }
                        return builder.build();
                    });

            AccountListResponse response = applyHeaders(spec, authorization)
                    .retrieve()
                    .body(AccountListResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-001 계좌 목록 조회"
            );

            return response;

        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "은행-001 계좌 목록 조회 호출 실패 (HTTP " + exception.getStatusCode() + "): " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("은행-001 계좌 목록 조회 호출 실패", exception);
        }
    }

    public DepositBasicResponse getDepositBasic(DepositBasicRequest request) {
        return getDepositBasic(null, request);
    }

    public DepositBasicResponse getDepositBasic(String authorization, DepositBasicRequest request) {
        try {
            String effectiveSearchTimestamp = (request != null && hasText(request.searchTimestamp()))
                    ? request.searchTimestamp()
                    : "0";

            DepositBasicRequest finalRequest = new DepositBasicRequest(
                    request != null ?  request.orgCode() : null,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    effectiveSearchTimestamp
            );

            var spec = restClient.post()
                    .uri("/v2/bank/accounts/deposit/basic")
                    .body(finalRequest);

            DepositBasicResponse response = applyHeaders(spec, authorization)
                    .retrieve()
                    .body(DepositBasicResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-002 수신계좌 기본정보 조회"
            );

            return response;

        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "은행-002 수신계좌 기본정보 조회 호출 실패 (HTTP " + exception.getStatusCode() + "): " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("은행-002 수신계좌 기본정보 조회 호출 실패", exception);
        }
    }

    public DepositDetailResponse getDepositDetail(DepositDetailRequest request) {
        return getDepositDetail(null, request);
    }

    public DepositDetailResponse getDepositDetail(String authorization, DepositDetailRequest request) {
        try {
            String effectiveSearchTimestamp = (request != null && hasText(request.searchTimestamp()))
                    ? request.searchTimestamp()
                    : "0";

            DepositDetailRequest finalRequest = new DepositDetailRequest(
                    request != null ? request.orgCode() : null,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    effectiveSearchTimestamp
            );

            var spec = restClient.post()
                    .uri("/v2/bank/accounts/deposit/detail")
                    .body(finalRequest);

            DepositDetailResponse response = applyHeaders(spec, authorization)
                    .retrieve()
                    .body(DepositDetailResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-003 수신계좌 추가정보 조회"
            );

            return response;

        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "은행-003 수신계좌 추가정보 조회 호출 실패 (HTTP " + exception.getStatusCode() + "): " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("은행-003 수신계좌 추가정보 조회 호출 실패", exception);
        }
    }

    public DepositTransactionResponse getDepositTransactions(DepositTransactionRequest request) {
        return getDepositTransactions(null, request);
    }

    public DepositTransactionResponse getDepositTransactions(String authorization, DepositTransactionRequest request) {
        try {
            int limit = (request != null && request.limit() > 0) ? request.limit() : 20;

            String defaultToDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String defaultFromDate = LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String effectiveFromDate = (request != null && hasText(request.fromDate())) ? request.fromDate() : defaultFromDate;
            String effectiveToDate = (request != null && hasText(request.toDate())) ? request.toDate() : defaultToDate;

            DepositTransactionRequest finalRequest = new DepositTransactionRequest(
                    request != null ? request.orgCode() : null,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    effectiveFromDate,
                    effectiveToDate,
                    request != null ? request.nextPage() : null,
                    limit
            );

            var spec = restClient.post()
                    .uri("/v2/bank/accounts/deposit/transactions")
                    .body(finalRequest);

            DepositTransactionResponse response = applyHeaders(spec, authorization)
                    .retrieve()
                    .body(DepositTransactionResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-004 수신계좌 거래내역 조회"
            );

            return response;

        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "은행-004 수신계좌 거래내역 조회 호출 실패 (HTTP " + exception.getStatusCode() + "): " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("은행-004 수신계좌 거래내역 조회 호출 실패", exception);
        }
    }

    private void validate(String rspCode, String rspMsg, String apiName) {
        if (rspCode == null) {
            throw new IllegalStateException(apiName + " 응답이 비어 있습니다.");
        }

        if (!SUCCESS_CODE.equals(rspCode)) {
            throw new IllegalStateException(apiName + " 실패: " + rspCode + " / " + rspMsg);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}