package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.service.LivingExpenseEstimator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalPresetServiceTest {

    @Mock
    private LivingExpenseEstimator livingExpenseEstimator;

    private GoalPresetService service() {
        return new GoalPresetService(livingExpenseEstimator);
    }

    @Test
    void 프리셋_4개를_반환한다() {
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));

        List<GoalPresetService.GoalPreset> presets = service().getPresets(1L);

        assertThat(presets).hasSize(4);
        assertThat(presets).extracting(GoalPresetService.GoalPreset::key)
                .containsExactly("JEONSE", "SEED_MONEY", "FINANCIAL_FREEDOM", "EMERGENCY_FUND");
    }

    @Test
    void 고정_프리셋_금액은_스펙값_그대로다() {
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));

        Map<String, BigDecimal> amountByKey = amountByKey(service().getPresets(1L));

        assertThat(amountByKey.get("JEONSE")).isEqualByComparingTo("200000000");
        assertThat(amountByKey.get("SEED_MONEY")).isEqualByComparingTo("100000000");
        assertThat(amountByKey.get("FINANCIAL_FREEDOM")).isEqualByComparingTo("1000000000");
    }

    @Test
    void 비상금_프리셋은_월평균_생활비의_6배다() {
        when(livingExpenseEstimator.estimate(1L, 3))
                .thenReturn(new LivingExpenseEstimator.Result(new BigDecimal("2000000"), true, "disclaimer"));

        Map<String, BigDecimal> amountByKey = amountByKey(service().getPresets(1L));

        assertThat(amountByKey.get("EMERGENCY_FUND")).isEqualByComparingTo("12000000");
    }

    private Map<String, BigDecimal> amountByKey(List<GoalPresetService.GoalPreset> presets) {
        return presets.stream()
                .collect(java.util.stream.Collectors.toMap(GoalPresetService.GoalPreset::key, GoalPresetService.GoalPreset::amount));
    }
}
