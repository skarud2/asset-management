package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeConditionParserTest {

    private final IncomeConditionParser parser = new IncomeConditionParser();

    @Test
    void evaluatesMonthlyIncomeOnlyOnce() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .annualIncome(BigDecimal.valueOf(48_000_000L))
                .build();

        List<ConditionEvaluation> results = parser.evaluate("월 소득 300만원 이하", user);

        assertThat(results)
                .filteredOn(result -> result.getType() == ConditionType.INCOME)
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }

    @Test
    void evaluatesAnnualIncomeMaximum() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .annualIncome(BigDecimal.valueOf(35_000_000L))
                .build();

        List<ConditionEvaluation> results = parser.evaluate("연소득 4,000만원 이하", user);

        assertThat(results)
                .anyMatch(result -> result.getType() == ConditionType.INCOME
                        && result.getStatus() == ConditionStatus.SATISFIED);
    }

    @Test
    void leavesHouseholdIncomeForConfirmation() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .annualIncome(BigDecimal.valueOf(30_000_000L))
                .build();

        List<ConditionEvaluation> results = parser.evaluate("가구소득 기준중위소득 80% 이하", user);

        assertThat(results)
                .anyMatch(result -> result.getType() == ConditionType.INCOME
                        && result.getStatus() == ConditionStatus.NEEDS_CONFIRMATION);
    }

    @Test
    void evaluatesNumericHouseholdIncomeLimit() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .annualIncome(BigDecimal.valueOf(30_000_000L))
                .householdAnnualIncome(BigDecimal.valueOf(55_000_000L))
                .build();

        List<ConditionEvaluation> results = parser.evaluate("부부합산 연소득 5,000만원 이하", user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getType()).isEqualTo(ConditionType.INCOME);
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.NOT_SATISFIED);
                    assertThat(result.getDescription()).contains("가구 합산");
                });
    }

    @Test
    void treatsSeventyMillionWonAndSeventyBaekmanWonAsSameHouseholdLimit() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdAnnualIncome(BigDecimal.valueOf(70_000_000L))
                .build();

        assertThat(parser.evaluate("본인과 배우자의 합산한 연소득이 70백만원 이하인 자", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);

        assertThat(parser.evaluate("본인과 배우자의 합산 소득이 7천만원 이하인 자", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);
    }

    @Test
    void doesNotInterpretNetAssetAsHouseholdIncome() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdAnnualIncome(BigDecimal.valueOf(30_000_000L))
                .build();

        assertThat(parser.evaluate("본인 및 배우자 합산 순자산 가액 5.06억 이하", user)).isEmpty();
    }

    @Test
    void extractsHouseholdIncomeFromLongHousingEligibilityText() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdAnnualIncome(BigDecimal.valueOf(65_000_000L))
                .build();
        String condition = "대출신청일 기준 만 34세 이하의 민법상 성년으로 무주택자인 세대주이며, "
                + "본인과 배우자의 합산 소득이 7천만원 이하인 자. 개인신용평점에 따른 제한은 없음";

        assertThat(parser.evaluate(condition, user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);
    }

    @Test
    void usesPersonalIncomeForSingleHouseholdWhenCombinedIncomeWasNotStored() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .maritalStatus("SINGLE")
                .annualIncome(BigDecimal.valueOf(45_000_000L))
                .build();

        assertThat(parser.evaluate("부부합산 연소득 5천만원 이하", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);
    }
}
