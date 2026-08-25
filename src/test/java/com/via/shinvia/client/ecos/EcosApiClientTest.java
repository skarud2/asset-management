package com.via.shinvia.client.ecos;

import com.via.shinvia.client.ecos.response.EcosResult;
import com.via.shinvia.client.ecos.response.EcosStatisticRow;
import com.via.shinvia.client.ecos.response.EcosStatisticSearchBody;
import com.via.shinvia.client.ecos.response.EcosStatisticSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 실제 ECOS 서버 호출 없이 RestTemplate을 모킹해서 페이지네이션/방어 로직만 검증
@ExtendWith(MockitoExtension.class)
class EcosApiClientTest {

    @Mock
    private RestTemplate ecosRestTemplate;

    private EcosApiProperties properties() {
        EcosApiProperties properties = new EcosApiProperties();
        properties.setKey("TEST-KEY");
        properties.setBaseUrl("https://ecos.bok.or.kr/api");
        return properties;
    }

    private EcosApiClient client() {
        return new EcosApiClient(ecosRestTemplate, properties());
    }

    private EcosStatisticRow row(String time, String value) {
        EcosStatisticRow row = new EcosStatisticRow();
        row.setTime(time);
        row.setDataValue(value);
        return row;
    }

    @Test
    void 요청건수_안에_다_들어오면_한_번만_호출한다() {
        EcosStatisticSearchResponse response = new EcosStatisticSearchResponse();
        response.setStatisticSearch(new EcosStatisticSearchBody(
                2, List.of(row("20240101", "3.50"), row("20240102", "3.50"))
        ));

        when(ecosRestTemplate.getForObject(any(URI.class), eq(EcosStatisticSearchResponse.class)))
                .thenReturn(response);

        List<EcosDailyRate> rates = client().getDailyBaseRate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        assertThat(rates).hasSize(2);
        verify(ecosRestTemplate, times(1)).getForObject(any(URI.class), eq(EcosStatisticSearchResponse.class));
    }

    @Test
    void list_total_count가_요청건수보다_많으면_전체건수로_늘려서_재요청한다() {
        EcosStatisticSearchResponse firstPage = new EcosStatisticSearchResponse();
        // 1일 요청(day=0 -> requestedEnd=100)인데 실제로는 150건이 있다고 응답
        firstPage.setStatisticSearch(new EcosStatisticSearchBody(150, List.of(row("20240101", "3.50"))));

        EcosStatisticSearchResponse secondPage = new EcosStatisticSearchResponse();
        secondPage.setStatisticSearch(new EcosStatisticSearchBody(150, List.of(
                row("20240101", "3.50"), row("20240102", "3.75")
        )));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        when(ecosRestTemplate.getForObject(uriCaptor.capture(), eq(EcosStatisticSearchResponse.class)))
                .thenReturn(firstPage)
                .thenReturn(secondPage);

        List<EcosDailyRate> rates = client().getDailyBaseRate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1));

        assertThat(rates).hasSize(2);
        verify(ecosRestTemplate, times(2)).getForObject(any(URI.class), eq(EcosStatisticSearchResponse.class));

        List<URI> calledUris = uriCaptor.getAllValues();
        assertThat(calledUris.get(0).toString()).contains("/1/100/");
        assertThat(calledUris.get(1).toString()).contains("/1/150/");
    }

    @Test
    void 조회된_데이터가_없다는_RESULT_코드면_빈_리스트를_반환한다() {
        EcosStatisticSearchResponse response = new EcosStatisticSearchResponse();
        EcosResult result = new EcosResult();
        result.setCode("INFO-200");
        result.setMessage("조회된 데이터가 없습니다");
        response.setResult(result);

        when(ecosRestTemplate.getForObject(any(URI.class), eq(EcosStatisticSearchResponse.class)))
                .thenReturn(response);

        List<EcosDailyRate> rates = client().getDailyBaseRate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        assertThat(rates).isEmpty();
    }

    @Test
    void 그_외_에러_코드면_EcosApiException을_던진다() {
        EcosStatisticSearchResponse response = new EcosStatisticSearchResponse();
        EcosResult result = new EcosResult();
        result.setCode("ERROR-100");
        result.setMessage("인증키가 유효하지 않습니다");
        response.setResult(result);

        when(ecosRestTemplate.getForObject(any(URI.class), eq(EcosStatisticSearchResponse.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> client().getDailyBaseRate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)))
                .isInstanceOf(EcosApiException.class)
                .hasMessageContaining("인증키가 유효하지 않습니다");
    }
}
