package com.via.shinvia.finprofile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeTypeTest {

    @Test
    void convertsLegacyKoreanValues() {
        assertThat(IncomeType.fromDatabaseValue("근로")).isEqualTo(IncomeType.EMPLOYMENT);
        assertThat(IncomeType.fromDatabaseValue("사업소득")).isEqualTo(IncomeType.BUSINESS);
        assertThat(IncomeType.fromDatabaseValue("금융")).isEqualTo(IncomeType.FINANCIAL);
        assertThat(IncomeType.fromDatabaseValue("기타소득")).isEqualTo(IncomeType.OTHER);
    }

    @Test
    void preservesCurrentEnumValues() {
        assertThat(IncomeType.fromDatabaseValue("EMPLOYMENT")).isEqualTo(IncomeType.EMPLOYMENT);
        assertThat(IncomeType.fromDatabaseValue(" business ")).isEqualTo(IncomeType.BUSINESS);
    }

    @Test
    void handlesEmptyAndUnknownValuesSafely() {
        assertThat(IncomeType.fromDatabaseValue(null)).isNull();
        assertThat(IncomeType.fromDatabaseValue(" ")).isNull();
        assertThat(IncomeType.fromDatabaseValue("새로운소득유형")).isEqualTo(IncomeType.OTHER);
    }
}
