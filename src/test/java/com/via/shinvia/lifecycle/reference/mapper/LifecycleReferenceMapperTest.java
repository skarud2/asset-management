package com.via.shinvia.lifecycle.reference.mapper;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.reference.dto.LifecycleReferenceDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "finance.api.service-key=test-key"
})
@Sql(scripts = "/db/lifecycle_reference_init.sql")
class LifecycleReferenceMapperTest {

    @Autowired
    private LifecycleReferenceMapper lifecycleReferenceMapper;

    @Test
    void 전국_결혼_평균비용을_조회한다() {

        LifecycleReferenceDto result =
                lifecycleReferenceMapper.findLatestReference(
                        LifecycleEventType.MARRIAGE,
                        "TOTAL_COST",
                        null,
                        null,
                        null
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getAmountValue())
                .isEqualByComparingTo(
                        new BigDecimal("21390000.00")
                );

        assertThat(result.getSourceName())
                .isEqualTo("한국소비자원");

        assertThat(result.getActive())
                .isTrue();
    }

    @Test
    void 결혼_평균형_비용배율을_조회한다() {

        LifecycleReferenceDto result =
                lifecycleReferenceMapper.findLatestReference(
                        LifecycleEventType.MARRIAGE,
                        "LIFESTYLE_COST_MULTIPLIER",
                        LifestyleLevel.AVERAGE,
                        null,
                        null
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getRateValue())
                .isEqualByComparingTo(
                        new BigDecimal("1.000000")
                );
    }
}