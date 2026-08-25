package com.via.shinvia.futuresim.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// "지금 내 상태"에서 가구원별 기준을 선택한 사용자에게 보여줄 프로필 요약 문장을 만든다.
// kosis_household_net_worth의 해당 가구원수 row(benchmark)와 '전체' row(overall)를
// HouseholdNetWorthBenchmarkService로 각각 조회해서, 소득 대비 원리금상환 부담(debtBurdenRatio)을
// 서로 비교한 문장을 조합한다. 연령대(AGE) 브랜치는 이번 스코프에 없음 — 나중에 별도 추가.
@Service
public class HouseholdProfileSummaryService {

    private static final String OVERALL_LABEL = null; // HouseholdNetWorthBenchmarkService.getBenchmark(null) => '전체'
    private static final BigDecimal SIMILAR_THRESHOLD_RATIO = new BigDecimal("0.05"); // 5% 이내면 "비슷한 수준"
    private static final int RATIO_SCALE = 6;

    private final HouseholdNetWorthBenchmarkService benchmarkService;

    public HouseholdProfileSummaryService(HouseholdNetWorthBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    public String generateSummary(String householdSizeLabel) {
        HouseholdNetWorthBenchmarkService.Result benchmark = benchmarkService.getBenchmark(householdSizeLabel);
        HouseholdNetWorthBenchmarkService.Result overall = benchmarkService.getBenchmark(OVERALL_LABEL);

        BigDecimal debtBurdenRatio = debtBurdenRatio(benchmark);
        BigDecimal overallDebtBurdenRatio = debtBurdenRatio(overall);

        String comparison = compare(debtBurdenRatio, overallDebtBurdenRatio);

        return "%s 가구는 전체 가구 중 %s%%를 차지해요. 소득 대비 원리금상환 부담은 전체 평균 대비 %s이에요."
                .formatted(
                        benchmark.householdSizeLabel(),
                        benchmark.householdDistributionPct().toPlainString(),
                        comparison
                );
    }

    private BigDecimal debtBurdenRatio(HouseholdNetWorthBenchmarkService.Result result) {
        BigDecimal income = result.avgIncome();
        if (income == null || income.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return result.avgDebtRepayment().divide(income, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private String compare(BigDecimal ratio, BigDecimal overallRatio) {
        if (overallRatio.signum() == 0) {
            return "비슷한 수준";
        }

        BigDecimal relativeDiff = ratio.subtract(overallRatio)
                .divide(overallRatio, RATIO_SCALE, RoundingMode.HALF_UP)
                .abs();

        if (relativeDiff.compareTo(SIMILAR_THRESHOLD_RATIO) <= 0) {
            return "비슷한 수준";
        }

        return ratio.compareTo(overallRatio) > 0 ? "높은 편" : "낮은 편";
    }
}
