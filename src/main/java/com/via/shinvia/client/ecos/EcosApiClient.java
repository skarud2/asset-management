package com.via.shinvia.client.ecos;

import com.via.shinvia.client.ecos.response.EcosResult;
import com.via.shinvia.client.ecos.response.EcosStatisticSearchBody;
import com.via.shinvia.client.ecos.response.EcosStatisticSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

// 한국은행 ECOS StatisticSearch API 클라이언트 (722Y001 시장금리 / 0101000 한국은행 기준금리, 일별)
@Component
@RequiredArgsConstructor
public class EcosApiClient {

    private static final String STAT_CODE = "722Y001";
    private static final String CYCLE = "D";
    private static final String ITEM_CODE = "0101000";
    private static final String NO_DATA_CODE_PREFIX = "INFO-200";

    private static final int START_INDEX = 1;
    // 요청 구간의 일수만큼은 최소 확보하고, 영업일 제외 등으로 인한 오차에 대비해 여유분을 더한다
    private static final int END_INDEX_BUFFER = 100;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestTemplate ecosRestTemplate;
    private final EcosApiProperties ecosApiProperties;

    public List<EcosDailyRate> getDailyBaseRate(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        int requestedEnd = (int) days + END_INDEX_BUFFER;

        EcosStatisticSearchBody body = fetchBody(startDate, endDate, START_INDEX, requestedEnd);

        // 한 번에 다 못 받아온 경우 -> 전체 건수(list_total_count)로 종료건수를 늘려서 재요청
        if (body.getListTotalCount() != null && body.getListTotalCount() > requestedEnd) {
            body = fetchBody(startDate, endDate, START_INDEX, body.getListTotalCount());
        }

        List<EcosDailyRate> rows = body.getRow() == null ? List.of() : body.getRow().stream()
                .map(row -> new EcosDailyRate(LocalDate.parse(row.getTime(), DATE_FORMAT), new BigDecimal(row.getDataValue())))
                .toList();

        return rows.stream()
                .sorted(Comparator.comparing(EcosDailyRate::date))
                .toList();
    }

    private EcosStatisticSearchBody fetchBody(LocalDate startDate, LocalDate endDate, int startIndex, int endIndex) {
        URI uri = UriComponentsBuilder.fromUriString(ecosApiProperties.getBaseUrl())
                .path("/StatisticSearch/{apiKey}/json/kr/{startIndex}/{endIndex}/{statCode}/{cycle}/{startDate}/{endDate}/{itemCode}")
                .buildAndExpand(
                        ecosApiProperties.getKey(),
                        startIndex,
                        endIndex,
                        STAT_CODE,
                        CYCLE,
                        startDate.format(DATE_FORMAT),
                        endDate.format(DATE_FORMAT),
                        ITEM_CODE
                )
                .toUri();

        EcosStatisticSearchResponse response = ecosRestTemplate.getForObject(uri, EcosStatisticSearchResponse.class);

        if (response == null) {
            throw new EcosApiException("응답이 비어있어요");
        }
        if (response.getStatisticSearch() != null) {
            return response.getStatisticSearch();
        }

        EcosResult result = response.getResult();
        if (result != null && result.getCode() != null && result.getCode().startsWith(NO_DATA_CODE_PREFIX)) {
            // 조회된 데이터가 없음 (정상 케이스로 취급, 빈 결과 반환)
            return new EcosStatisticSearchBody(0, List.of());
        }

        String message = result != null ? result.getCode() + " " + result.getMessage() : "알 수 없는 응답 형식이에요";
        throw new EcosApiException(message);
    }
}
