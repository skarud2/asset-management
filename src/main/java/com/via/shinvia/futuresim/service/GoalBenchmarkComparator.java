package com.via.shinvia.futuresim.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 목표 금액을 1단계에서 선택한 기준(연령대/가구원별)의 중앙값 순자산과 비교하는 문구를 만든다.
// benchmarkLabel은 스펙의 "[기준]" 자리에 그대로 들어간다(연령대 라벨 또는 가구원수 라벨) —
// 호출부(FutureGoalApiController)가 세션의 선택 기준에 맞는 라벨/중앙값을 넘겨준다.
@Service
public class GoalBenchmarkComparator {

    private static final int PERCENT_SCALE = 1;

    public String compareToBenchmark(BigDecimal goalAmount, BigDecimal benchmarkMedianNetWorth, String benchmarkLabel) {
        if (benchmarkMedianNetWorth == null || benchmarkMedianNetWorth.signum() == 0) {
            return null;
        }

        BigDecimal percentDiff = goalAmount.subtract(benchmarkMedianNetWorth)
                .divide(benchmarkMedianNetWorth, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);

        if (percentDiff.signum() > 0) {
            return "이 목표는 %s 중앙값 순자산보다 %s%% 높은 목표예요".formatted(benchmarkLabel, percentDiff.toPlainString());
        }

        return "이미 %s 중앙값 순자산 수준이에요".formatted(benchmarkLabel);
    }
}
