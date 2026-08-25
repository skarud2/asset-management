package com.via.shinvia.futuresim.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdProfileSummaryServiceTest {

    @Mock
    private HouseholdNetWorthBenchmarkService benchmarkService;

    private HouseholdProfileSummaryService service() {
        return new HouseholdProfileSummaryService(benchmarkService);
    }

    private HouseholdNetWorthBenchmarkService.Result result(
            String label, BigDecimal distributionPct, BigDecimal avgDebtRepayment, BigDecimal avgIncome
    ) {
        return new HouseholdNetWorthBenchmarkService.Result(
                "B0000", label, "2025",
                avgIncome, avgIncome,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                avgDebtRepayment, avgDebtRepayment,
                null, distributionPct,
                false
        );
    }

    @Test
    void 일인가구_요약문장이_실제_DB값_기준_계산과_일치한다() {
        when(benchmarkService.getBenchmark("1인"))
                .thenReturn(result("1인", new BigDecimal("32.05"), new BigDecimal("5788937.76"), new BigDecimal("34232926.00")));
        when(benchmarkService.getBenchmark(null))
                .thenReturn(result("전체", new BigDecimal("100.00"), new BigDecimal("13276190.36"), new BigDecimal("74273161.00")));

        String summary = service().generateSummary("1인");

        // 1인 ratio=0.169104, 전체 ratio=0.178748, 상대차이 약 5.395% > 5% => "낮은 편"
        assertThat(summary).isEqualTo("1인 가구는 전체 가구 중 32.05%를 차지해요. 소득 대비 원리금상환 부담은 전체 평균 대비 낮은 편이에요.");
    }

    @Test
    void 부담비율_차이가_5퍼센트_이내면_비슷한_수준으로_분류된다() {
        // benchmark ratio = 1000/10000 = 0.10, overall ratio = 1030/10000 = 0.103 => 상대차이 약 2.91% (5% 이내)
        when(benchmarkService.getBenchmark("2인"))
                .thenReturn(result("2인", new BigDecimal("28.58"), new BigDecimal("1000"), new BigDecimal("10000")));
        when(benchmarkService.getBenchmark(null))
                .thenReturn(result("전체", new BigDecimal("100.00"), new BigDecimal("1030"), new BigDecimal("10000")));

        String summary = service().generateSummary("2인");

        assertThat(summary).contains("비슷한 수준");
    }

    @Test
    void 부담비율이_전체보다_뚜렷이_높으면_높은_편으로_분류된다() {
        // benchmark ratio = 2000/10000 = 0.20, overall ratio = 1000/10000 = 0.10 => 상대차이 100%
        when(benchmarkService.getBenchmark("4인"))
                .thenReturn(result("4인", new BigDecimal("15.16"), new BigDecimal("2000"), new BigDecimal("10000")));
        when(benchmarkService.getBenchmark(null))
                .thenReturn(result("전체", new BigDecimal("100.00"), new BigDecimal("1000"), new BigDecimal("10000")));

        String summary = service().generateSummary("4인");

        assertThat(summary).contains("높은 편");
    }
}
