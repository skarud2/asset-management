package com.via.shinvia.futuresim.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GoalBenchmarkComparatorTest {

    private final GoalBenchmarkComparator comparator = new GoalBenchmarkComparator();

    @Test
    void 목표금액이_기준_중앙값보다_높으면_높은_목표라는_문구를_반환한다() {
        String text = comparator.compareToBenchmark(
                new BigDecimal("300000000"), new BigDecimal("200000000"), "30~39세"
        );

        assertThat(text).contains("30~39세").contains("50.0%").contains("높은 목표예요");
    }

    @Test
    void 목표금액이_기준_중앙값_이하이면_이미_그_수준이라는_문구를_반환한다() {
        String text = comparator.compareToBenchmark(
                new BigDecimal("100000000"), new BigDecimal("200000000"), "1인"
        );

        assertThat(text).contains("1인").contains("이미").contains("수준이에요");
    }

    @Test
    void 기준_중앙값이_없으면_null을_반환한다() {
        String text = comparator.compareToBenchmark(new BigDecimal("100000000"), null, "1인");

        assertThat(text).isNull();
    }
}
