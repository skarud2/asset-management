package com.via.shinvia.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.via.shinvia.lifecycle.common.model.CurrentHousingType;
import com.via.shinvia.lifecycle.common.model.SalaryGrowthScenario;
import com.via.shinvia.lifecycle.survey.dto.LifecycleBaseSurveyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleBaseSurveyRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Spring Boot 4 tools.jackson으로 JSON 역직렬화 시 모든 필드가 정상 파싱된다")
    void deserializeWithToolsJackson() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000,
                    "currentHousingType": "MONTHLY_RENT",
                    "monthlyHousingExpense": 500000,
                    "industryCode": "IT",
                    "salaryGrowthScenario": "CUSTOM",
                    "customSalaryGrowthRate": 0.03
                }
                """;

        LifecycleBaseSurveyRequest request = jsonMapper.readValue(json, LifecycleBaseSurveyRequest.class);

        assertThat(request.getMonthlyLivingExpense()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(request.getMonthlyHousingExpense()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(request.getSalaryGrowthScenario()).isEqualTo(SalaryGrowthScenario.CUSTOM);
        assertThat(request.getCustomSalaryGrowthRate()).isEqualByComparingTo(new BigDecimal("0.03"));
        assertThat(request.getCurrentHousingType()).isEqualTo(CurrentHousingType.MONTHLY_RENT);
        assertThat(request.getIndustryCode()).isEqualTo(com.via.shinvia.lifecycle.common.model.IndustryCode.IT);
    }

    @Test
    @DisplayName("JSON 역직렬화 시 salaryGrowthScenario가 Enum 문자열로 올 때 정상 파싱된다")
    void deserializeWithEnumString() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000,
                    "currentHousingType": "MONTHLY_RENT",
                    "monthlyHousingExpense": 500000,
                    "industryCode": "IT",
                    "salaryGrowthScenario": "CUSTOM",
                    "customSalaryGrowthRate": 0.03
                }
                """;

        LifecycleBaseSurveyRequest request = objectMapper.readValue(json, LifecycleBaseSurveyRequest.class);

        assertThat(request.getMonthlyLivingExpense()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(request.getMonthlyHousingExpense()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(request.getSalaryGrowthScenario()).isEqualTo(SalaryGrowthScenario.CUSTOM);
        assertThat(request.getCustomSalaryGrowthRate()).isEqualByComparingTo(new BigDecimal("0.03"));
        assertThat(request.getCurrentHousingType()).isEqualTo(CurrentHousingType.MONTHLY_RENT);
    }

    @Test
    @DisplayName("JSON 역직렬화 시 salaryGrowthScenario가 한글 라벨로 올 때도 정상 파싱된다")
    void deserializeWithKoreanLabel() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000,
                    "currentHousingType": "월세",
                    "salaryGrowthScenario": "직접 입력",
                    "customSalaryGrowthRate": 0.035
                }
                """;

        LifecycleBaseSurveyRequest request = objectMapper.readValue(json, LifecycleBaseSurveyRequest.class);

        assertThat(request.getSalaryGrowthScenario()).isEqualTo(SalaryGrowthScenario.CUSTOM);
        assertThat(request.getCurrentHousingType()).isEqualTo(CurrentHousingType.MONTHLY_RENT);
    }

    @Test
    @DisplayName("salaryGrowthScenario가 누락되었지만 customSalaryGrowthRate가 있는 경우 CUSTOM으로 자동 보정된다")
    void fallbackToCustomWhenRateExists() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000,
                    "customSalaryGrowthRate": 0.05
                }
                """;

        LifecycleBaseSurveyRequest request = objectMapper.readValue(json, LifecycleBaseSurveyRequest.class);

        assertThat(request.getSalaryGrowthScenario()).isEqualTo(SalaryGrowthScenario.CUSTOM);
    }

    @Test
    @DisplayName("salaryGrowthScenario와 customSalaryGrowthRate 모두 없을 경우 BASE 기본값이 반환된다")
    void fallbackToBaseWhenNothingProvided() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000
                }
                """;

        LifecycleBaseSurveyRequest request = objectMapper.readValue(json, LifecycleBaseSurveyRequest.class);

        assertThat(request.getSalaryGrowthScenario()).isEqualTo(SalaryGrowthScenario.BASE);
    }

    @Test
    @DisplayName("JSON 역직렬화 시 industryCode가 Enum명 또는 한글 라벨로 올 때 정상 파싱된다")
    void deserializeIndustryCode() throws Exception {
        String json1 = """
                {
                    "industryCode": "FINANCE"
                }
                """;
        LifecycleBaseSurveyRequest request1 = objectMapper.readValue(json1, LifecycleBaseSurveyRequest.class);
        assertThat(request1.getIndustryCode()).isEqualTo(com.via.shinvia.lifecycle.common.model.IndustryCode.FINANCE);

        String json2 = """
                {
                    "industryCode": "IT · 정보통신"
                }
                """;
        LifecycleBaseSurveyRequest request2 = objectMapper.readValue(json2, LifecycleBaseSurveyRequest.class);
        assertThat(request2.getIndustryCode()).isEqualTo(com.via.shinvia.lifecycle.common.model.IndustryCode.IT);
    }

    @Test
    @DisplayName("industryCode가 누락되었을 경우 기본값 ETC가 반환된다")
    void fallbackToEtcWhenIndustryCodeMissing() throws Exception {
        String json = """
                {
                    "monthlyLivingExpense": 1500000
                }
                """;
        LifecycleBaseSurveyRequest request = objectMapper.readValue(json, LifecycleBaseSurveyRequest.class);
        assertThat(request.getIndustryCode()).isEqualTo(com.via.shinvia.lifecycle.common.model.IndustryCode.ETC);
    }
}
