package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.entity.KosisAgeGroupNetWorth;
import com.via.shinvia.futuresim.entity.KosisHouseholdNetWorth;
import com.via.shinvia.futuresim.exception.InvalidAgeException;
import com.via.shinvia.futuresim.exception.InvalidHouseholdSizeException;
import com.via.shinvia.futuresim.mapper.AgeGroupNetWorthBenchmarkMapper;
import com.via.shinvia.futuresim.mapper.HouseholdNetWorthBenchmarkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdNetWorthBenchmarkServiceTest {

    @Mock
    private HouseholdNetWorthBenchmarkMapper benchmarkMapper;

    @Mock
    private AgeGroupNetWorthBenchmarkMapper ageGroupBenchmarkMapper;

    private HouseholdNetWorthBenchmarkService service() {
        return new HouseholdNetWorthBenchmarkService(benchmarkMapper, ageGroupBenchmarkMapper);
    }

    private KosisHouseholdNetWorth row(String code, String label, BigDecimal avgNetWorth, BigDecimal medianNetWorth) {
        KosisHouseholdNetWorth row = new KosisHouseholdNetWorth();
        row.setHouseholdSizeCode(code);
        row.setHouseholdSizeLabel(label);
        row.setSurveyYear("2025");
        row.setAvgNetWorth(avgNetWorth);
        row.setMedianNetWorth(medianNetWorth);
        return row;
    }

    private KosisAgeGroupNetWorth ageGroupRow(String code, String label, BigDecimal avgNetWorth, BigDecimal medianNetWorth) {
        KosisAgeGroupNetWorth row = new KosisAgeGroupNetWorth();
        row.setAgeGroupCode(code);
        row.setAgeGroupLabel(label);
        row.setSurveyYear("2025");
        row.setAvgNetWorth(avgNetWorth);
        row.setMedianNetWorth(medianNetWorth);
        return row;
    }

    @Test
    void 가구원수_1인이면_해당_라벨의_벤치마크를_그대로_반환한다() {
        when(benchmarkMapper.findLatestByLabel("1인"))
                .thenReturn(row("B0601", "1인", new BigDecimal("182838245"), new BigDecimal("74000000")));

        HouseholdNetWorthBenchmarkService.Result result = service().getBenchmark("1인");

        assertThat(result.householdSizeLabel()).isEqualTo("1인");
        assertThat(result.avgNetWorth()).isEqualByComparingTo("182838245");
        assertThat(result.medianNetWorth()).isEqualByComparingTo("74000000");
        assertThat(result.isDefault()).isFalse();
    }

    @Test
    void 가구원수_미입력이면_전체_벤치마크로_폴백하고_isDefault가_true다() {
        when(benchmarkMapper.findLatestByLabel("전체"))
                .thenReturn(row("B0600", "전체", new BigDecimal("471437032"), new BigDecimal("238600000")));

        HouseholdNetWorthBenchmarkService.Result result = service().getBenchmark(null);

        assertThat(result.householdSizeLabel()).isEqualTo("전체");
        assertThat(result.avgNetWorth()).isEqualByComparingTo("471437032");
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void 존재하지_않는_가구원수_값이면_예외를_던진다() {
        when(benchmarkMapper.findLatestByLabel("10인")).thenReturn(null);

        assertThatThrownBy(() -> service().getBenchmark("10인"))
                .isInstanceOf(InvalidHouseholdSizeException.class)
                .hasMessageContaining("10인");
    }

    @Test
    void 나이_35세면_30대_구간_벤치마크를_반환한다() {
        when(ageGroupBenchmarkMapper.findLatestByLabel("30~39세"))
                .thenReturn(ageGroupRow("B1602", "30~39세", new BigDecimal("250596721.82"), new BigDecimal("155850000")));

        HouseholdNetWorthBenchmarkService.AgeGroupResult result = service().getAgeGroupBenchmark(35);

        assertThat(result.ageGroupLabel()).isEqualTo("30~39세");
        assertThat(result.avgNetWorth()).isEqualByComparingTo("250596721.82");
    }

    @Test
    void 나이_29세는_29세_이하_구간으로_분류된다() {
        when(ageGroupBenchmarkMapper.findLatestByLabel("29세 이하"))
                .thenReturn(ageGroupRow("B1601", "29세 이하", new BigDecimal("107963767.18"), new BigDecimal("50000000")));

        HouseholdNetWorthBenchmarkService.AgeGroupResult result = service().getAgeGroupBenchmark(29);

        assertThat(result.ageGroupLabel()).isEqualTo("29세 이하");
    }

    @Test
    void 나이_30세는_30대_구간으로_분류된다() {
        when(ageGroupBenchmarkMapper.findLatestByLabel("30~39세"))
                .thenReturn(ageGroupRow("B1602", "30~39세", new BigDecimal("250596721.82"), new BigDecimal("155850000")));

        HouseholdNetWorthBenchmarkService.AgeGroupResult result = service().getAgeGroupBenchmark(30);

        assertThat(result.ageGroupLabel()).isEqualTo("30~39세");
    }

    @Test
    void 나이_60세는_60세_이상_구간으로_분류된다() {
        when(ageGroupBenchmarkMapper.findLatestByLabel("60세 이상"))
                .thenReturn(ageGroupRow("B1605", "60세 이상", new BigDecimal("535912911.70"), new BigDecimal("250000000")));

        HouseholdNetWorthBenchmarkService.AgeGroupResult result = service().getAgeGroupBenchmark(60);

        assertThat(result.ageGroupLabel()).isEqualTo("60세 이상");
    }

    @Test
    void 음수_나이는_예외를_던진다() {
        assertThatThrownBy(() -> service().getAgeGroupBenchmark(-1))
                .isInstanceOf(InvalidAgeException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void 가구원수_1인이면_원리금상환액도_같이_반환한다() {
        KosisHouseholdNetWorth row = row("B0601", "1인", new BigDecimal("182838245"), new BigDecimal("74000000"));
        row.setAvgDebtRepayment(new BigDecimal("5788937.76"));
        row.setMedianDebtRepayment(new BigDecimal("6000000"));
        when(benchmarkMapper.findLatestByLabel("1인")).thenReturn(row);

        HouseholdNetWorthBenchmarkService.Result result = service().getBenchmark("1인");

        assertThat(result.avgDebtRepayment()).isEqualByComparingTo("5788937.76");
        assertThat(result.medianDebtRepayment()).isEqualByComparingTo("6000000");
    }

    @Test
    void 연령대_벤치마크도_원리금상환액을_같이_반환한다() {
        KosisAgeGroupNetWorth row = ageGroupRow("B1602", "30~39세", new BigDecimal("250596721.82"), new BigDecimal("155850000"));
        row.setAvgDebtRepayment(new BigDecimal("16457212.87"));
        row.setMedianDebtRepayment(new BigDecimal("9840000"));
        when(ageGroupBenchmarkMapper.findLatestByLabel("30~39세")).thenReturn(row);

        HouseholdNetWorthBenchmarkService.AgeGroupResult result = service().getAgeGroupBenchmark(35);

        assertThat(result.avgDebtRepayment()).isEqualByComparingTo("16457212.87");
        assertThat(result.medianDebtRepayment()).isEqualByComparingTo("9840000");
    }
}
