package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.mapper.PlanSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanSnapshotServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PlanSnapshotMapper mapper;

    @Captor
    private ArgumentCaptor<String> jsonCaptor;

    private PlanSnapshotService service() {
        return new PlanSnapshotService(mapper);
    }

    @Test
    void 레버가_없어도_빈_배열로_저장된다() {
        service().save(USER_ID, null, new BigDecimal("100000000"), null, null, null, null, new BigDecimal("3.080"), List.of(), 100, 100, 0, null, new BigDecimal("5000000"));

        verify(mapper).upsert(
                eqLong(USER_ID), org.mockito.ArgumentMatchers.eq("계획 " + LocalDate.now()), eqAmount("100000000"), eqNull(), eqNull(), eqNull(), org.mockito.ArgumentMatchers.<BigDecimal>isNull(), eqAmount("3.080"), jsonCaptor.capture(), eqInt(100), eqInt(100), eqInt(0), eqNull(), eqAmount("5000000")
        );
        assertThat(jsonCaptor.getValue()).isEqualTo("[]");
    }

    @Test
    void 이름을_비워두면_날짜_기반_기본값이_붙는다() {
        String returnedName = service().save(USER_ID, "   ", new BigDecimal("100000000"), null, null, null, null, new BigDecimal("3.080"), List.of(), 100, 100, 0, null, new BigDecimal("5000000"));

        assertThat(returnedName).isEqualTo("계획 " + LocalDate.now());
    }

    @Test
    void 선택한_레버가_JSON_배열로_직렬화된다() {
        List<LeverIntensityCalculator.LeverSelection> selections = List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("20")),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, new BigDecimal("30000000"))
        );

        String returnedName = service().save(USER_ID, "내 집 마련 계획", new BigDecimal("100000000"), "SEED_MONEY", "AGE", "30~39세", new BigDecimal("155850000"), new BigDecimal("3.080"), selections, 120, 80, 40, "[]", new BigDecimal("100000000"));

        verify(mapper).upsert(
                eqLong(USER_ID), org.mockito.ArgumentMatchers.eq("내 집 마련 계획"), eqAmount("100000000"), org.mockito.ArgumentMatchers.eq("SEED_MONEY"),
                org.mockito.ArgumentMatchers.eq("AGE"), org.mockito.ArgumentMatchers.eq("30~39세"), eqAmount("155850000"), eqAmount("3.080"),
                jsonCaptor.capture(), eqInt(120), eqInt(80), eqInt(40), org.mockito.ArgumentMatchers.eq("[]"), eqAmount("100000000")
        );
        assertThat(jsonCaptor.getValue())
                .contains("\"leverType\":\"INCOME_CHANGE\"")
                .contains("\"leverType\":\"LOAN_PREPAYMENT\"")
                .contains("\"intensity\":30000000");
        assertThat(returnedName).isEqualTo("내 집 마련 계획");
    }

    private Long eqLong(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private BigDecimal eqAmount(String value) {
        return org.mockito.ArgumentMatchers.eq(new BigDecimal(value));
    }

    private int eqInt(int value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private String eqNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
