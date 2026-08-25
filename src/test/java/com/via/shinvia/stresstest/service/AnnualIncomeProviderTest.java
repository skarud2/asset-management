package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestFinancialProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualIncomeProviderTest {

    @Mock
    private StressTestFinancialProfileMapper financialProfileMapper;

    private AnnualIncomeProvider provider() {
        return new AnnualIncomeProvider(financialProfileMapper);
    }

    @Test
    void 재무_프로필이_있으면_연소득을_그대로_반환한다() {
        when(financialProfileMapper.findAnnualIncomeByUserId(1L))
                .thenReturn(new BigDecimal("48000000"));

        AnnualIncomeProvider.Result result = provider().findAnnualIncome(1L);

        assertThat(result.available()).isTrue();
        assertThat(result.annualIncome()).isEqualByComparingTo("48000000");
    }

    @Test
    void 재무_프로필이_없으면_데이터_없음으로_반환한다() {
        when(financialProfileMapper.findAnnualIncomeByUserId(2L)).thenReturn(null);

        AnnualIncomeProvider.Result result = provider().findAnnualIncome(2L);

        assertThat(result.available()).isFalse();
        assertThat(result.annualIncome()).isNull();
    }
}
