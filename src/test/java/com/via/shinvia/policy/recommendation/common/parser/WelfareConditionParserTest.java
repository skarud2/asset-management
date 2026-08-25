package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareConditionParserTest {

    private static final String WELCOME_STEPPING_STONE_TARGET =
            "기초생활수급자, 소년소녀가장, 북한이탈주민, 차상위계층, "
                    + "근로장려금 수급자, 한부모가족, 장애수당 대상자";
    private static final String NH_HOPE_TARGET =
            "기초생활수급자, 차상위계층, 한부모가족지원대상자, 북한이탈주민, "
                    + "장애인연금·장애수당·장애아동수당 수급자, 근로장려금 수급자, "
                    + "결혼이민자, 기초연금수급자";

    private final WelfareConditionParser parser = new WelfareConditionParser();

    @Test
    void satisfiesProductWhenKnownSurveyConditionMatches() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .basicLivelihoodRecipient(true)
                .build();

        List<ConditionEvaluation> results = parser.evaluate(WELCOME_STEPPING_STONE_TARGET, user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getType()).isEqualTo(ConditionType.WELFARE);
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                    assertThat(result.getDescription()).contains("기초생활수급자");
                });
    }

    @Test
    void rejectsWhenNoSurveyedWelfareConditionMatches() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .basicLivelihoodRecipient(false)
                .nearPoverty(false)
                .singleParentHousehold(false)
                .disabled(false)
                .build();

        List<ConditionEvaluation> results = parser.evaluate(WELCOME_STEPPING_STONE_TARGET, user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.NOT_SATISFIED);
                    assertThat(result.getDescription()).contains("해당하지 않습니다");
                });
    }

    @Test
    void satisfiesEarnedIncomeTaxCreditRecipient() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .earnedIncomeTaxCreditRecipient(true)
                .build();

        List<ConditionEvaluation> results = parser.evaluate(WELCOME_STEPPING_STONE_TARGET, user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                    assertThat(result.getDescription()).contains("근로장려금");
                });
    }

    @Test
    void keepsTrulyUnsurveyedAlternativeForConfirmation() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .basicLivelihoodRecipient(false)
                .build();

        List<ConditionEvaluation> results = parser.evaluate("기초생활수급자 또는 보호아동", user);

        assertThat(results)
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NEEDS_CONFIRMATION);
    }

    @Test
    void rejectsNhHopeProductWhenNoTargetConditionMatches() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .basicLivelihoodRecipient(false)
                .nearPoverty(false)
                .singleParentHousehold(false)
                .disabled(false)
                .northKoreanDefector(false)
                .earnedIncomeTaxCreditRecipient(false)
                .multiculturalHousehold(false)
                .basicPensionRecipient(false)
                .disabilityBenefitRecipient(false)
                .build();

        List<ConditionEvaluation> results = parser.evaluate(NH_HOPE_TARGET, user);

        assertThat(results)
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }

    @Test
    void doesNotTreatGenericDisabilityAsDisabilityBenefitReceipt() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .disabled(true)
                .disabilityBenefitRecipient(false)
                .build();

        List<ConditionEvaluation> results = parser.evaluate("장애인연금·장애수당 수급자", user);

        assertThat(results)
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }

    @Test
    void satisfiesBasicPensionRecipient() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .basicPensionRecipient(true)
                .build();

        List<ConditionEvaluation> results = parser.evaluate(NH_HOPE_TARGET, user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                    assertThat(result.getDescription()).contains("기초연금");
                });
    }

    @Test
    void evaluatesJeonseFraudVictimAsSurveyedCondition() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .jeonseFraudVictim(true)
                .build();

        List<ConditionEvaluation> results = parser.evaluate("전세피해자로 결정된 자", user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                    assertThat(result.getDescription()).contains("전세사기 피해자");
                });
    }
}
