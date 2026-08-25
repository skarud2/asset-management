package com.via.shinvia.client.ecos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 한국은행 ECOS 서버를 호출하는 통합 테스트.
// ECOS_API_KEY 환경변수가 없는 CI 환경에서는 자동으로 건너뛴다 (application.yml에 키가 있어도
// 이 테스트는 Spring 컨텍스트를 띄우지 않고 클라이언트를 직접 생성해서 호출한다).
@EnabledIfEnvironmentVariable(named = "ECOS_API_KEY", matches = ".+")
class EcosApiClientIntegrationTest {

    private EcosApiClient client() {
        EcosApiProperties properties = new EcosApiProperties();
        properties.setKey(System.getenv("ECOS_API_KEY"));
        properties.setBaseUrl("https://ecos.bok.or.kr/api");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(3000));
        requestFactory.setReadTimeout(Duration.ofMillis(5000));

        return new EcosApiClient(new RestTemplate(requestFactory), properties);
    }

    @Test
    void 짧은_기간을_조회하면_32건_이상_날짜순으로_반환된다() {
        List<EcosDailyRate> rates = client().getDailyBaseRate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)
        );

        assertThat(rates.size()).isGreaterThanOrEqualTo(32);
        assertThat(rates).isSortedAccordingTo(Comparator.comparing(EcosDailyRate::date));
    }

    @Test
    void 긴_기간을_조회해도_전부_받아온다() {
        List<EcosDailyRate> rates = client().getDailyBaseRate(
                LocalDate.of(2021, 8, 1), LocalDate.of(2023, 1, 31)
        );

        assertThat(rates).isNotEmpty();
        assertThat(rates.size()).isGreaterThan(500);
    }
}
