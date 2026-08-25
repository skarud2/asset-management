package com.via.shinvia.finprofile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmploymentStatusTest {

    @Test
    void convertsLegacyEmploymentValues() {
        assertThat(EmploymentStatus.fromDatabaseValue("재직")).isEqualTo(EmploymentStatus.REGULAR);
        assertThat(EmploymentStatus.fromDatabaseValue("EMPLOYED")).isEqualTo(EmploymentStatus.REGULAR);
        assertThat(EmploymentStatus.fromDatabaseValue("SELF_EMPLOYED")).isEqualTo(EmploymentStatus.BUSINESS);
        assertThat(EmploymentStatus.fromDatabaseValue("JOB_SEEKER")).isEqualTo(EmploymentStatus.UNEMPLOYED);
    }

    @Test
    void preservesCurrentEnumValues() {
        assertThat(EmploymentStatus.fromDatabaseValue("CONTRACT")).isEqualTo(EmploymentStatus.CONTRACT);
        assertThat(EmploymentStatus.fromDatabaseValue(" student ")).isEqualTo(EmploymentStatus.STUDENT);
    }

    @Test
    void handlesEmptyAndUnknownValuesSafely() {
        assertThat(EmploymentStatus.fromDatabaseValue(null)).isNull();
        assertThat(EmploymentStatus.fromDatabaseValue(" ")).isNull();
        assertThat(EmploymentStatus.fromDatabaseValue("새로운고용상태")).isEqualTo(EmploymentStatus.OTHER);
    }
}
