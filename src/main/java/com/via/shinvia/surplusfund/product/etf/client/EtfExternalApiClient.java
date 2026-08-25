package com.via.shinvia.surplusfund.product.etf.client;

import com.via.shinvia.surplusfund.product.etf.dto.EtfExternalResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
public class EtfExternalApiClient {

    private static final DateTimeFormatter BASIC_DATE =
            DateTimeFormatter.BASIC_ISO_DATE;

    private static final Set<String> NORMAL_CODES =
            Set.of("00", "0", "0000");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String serviceKey;

    public EtfExternalApiClient(
            @Qualifier("etfRestTemplate")
            RestTemplate restTemplate,

            @Value(
                    "${external-api.etf.base-url:"
                            + "https://apis.data.go.kr/1160100/service/"
                            + "GetSecuritiesProductInfoService}"
            )
            String baseUrl,

            @Value("${external-api.etf.service-key:}")
            String serviceKey
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = requireText(baseUrl, "ETF API base URL");

        // Encoding 키와 Decoding 키를 모두 받을 수 있도록 정규화한다.
        this.serviceKey = normalizeServiceKey(serviceKey);
    }

    public EtfPage fetchPage(
            LocalDate baseDate,
            int pageNo,
            int numOfRows
    ) {
        if (baseDate == null) {
            throw new IllegalArgumentException(
                    "ETF 기준일자는 필수입니다."
            );
        }

        if (pageNo < 1 || numOfRows < 1 || numOfRows > 1000) {
            throw new IllegalArgumentException(
                    "ETF API 페이지 값이 올바르지 않습니다."
            );
        }

        if (serviceKey.isBlank()) {
            throw new EtfExternalApiException(
                    "FSC_ETF_API_SERVICE_KEY가 설정되지 않았습니다."
            );
        }


        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .pathSegment("getETFPriceInfo")
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("resultType", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam(
                        "basDt",
                        BASIC_DATE.format(baseDate)
                )
                .encode()
                .buildAndExpand(serviceKey)
                .toUri();

        try {
            EtfExternalResponse result =
                    restTemplate.getForObject(
                            uri,
                            EtfExternalResponse.class
                    );

            return validateAndExtract(
                    result,
                    pageNo,
                    numOfRows
            );

        } catch (RestClientException exception) {
            throw new EtfExternalApiException(
                    "금융위원회 ETF API 호출에 실패했습니다.",
                    exception
            );
        }
    }

    private EtfPage validateAndExtract(
            EtfExternalResponse result,
            int requestedPageNo,
            int requestedNumOfRows
    ) {
        if (result == null) {
            throw new EtfExternalApiException(
                    "금융위원회 ETF API 응답이 비어 있습니다."
            );
        }

        EtfExternalResponse.Header header =
                result.resolvedHeader();

        if (header == null
                || header.resultCode() == null
                || !NORMAL_CODES.contains(
                header.resultCode().trim()
        )) {
            String resultCode =
                    header == null
                            ? "UNKNOWN"
                            : header.resultCode();

            String resultMessage =
                    header == null
                            ? "응답 헤더 없음"
                            : header.resultMsg();

            throw new EtfExternalApiException(
                    "금융위원회 ETF API 오류: "
                            + resultCode
                            + " / "
                            + resultMessage
            );
        }

        EtfExternalResponse.Body body =
                result.resolvedBody();

        if (body == null) {
            return new EtfPage(
                    requestedPageNo,
                    requestedNumOfRows,
                    0,
                    List.of()
            );
        }

        List<EtfExternalResponse.Item> items =
                body.items() == null
                        || body.items().item() == null
                        ? List.of()
                        : List.copyOf(
                        body.items().item()
                );

        return new EtfPage(
                body.pageNo() == null
                        ? requestedPageNo
                        : body.pageNo(),

                body.numOfRows() == null
                        ? requestedNumOfRows
                        : body.numOfRows(),

                body.totalCount() == null
                        ? items.size()
                        : body.totalCount(),

                items
        );
    }

    private static String normalizeServiceKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        // 복사 과정에서 들어간 줄바꿈과 공백을 제거한다.
        String normalized = value.replaceAll("\\s+", "");

        // 포털의 Encoding 키라면 디코딩한다.
        // URI 생성 시 encode()에서 다시 정확히 한 번 인코딩된다.
        if (normalized.contains("%")) {
            return URLDecoder.decode(
                    normalized,
                    StandardCharsets.UTF_8
            );
        }

        // 이미 Decoding 키라면 그대로 사용한다.
        return normalized;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "이 설정되지 않았습니다."
            );
        }

        return value.trim();
    }

    public record EtfPage(
            int pageNo,
            int numOfRows,
            int totalCount,
            List<EtfExternalResponse.Item> items
    ) {
    }
}