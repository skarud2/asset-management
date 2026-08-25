package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreditConditionParserTest {

    private final CreditConditionParser parser = new CreditConditionParser();

    @Test
    void satisfiesCbMinimumScore() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .creditScore(700)
                .build();

        List<ConditionEvaluation> results = parser.evaluate("CB점수 271점 이상", user);

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getType()).isEqualTo(ConditionType.CREDIT);
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                    assertThat(result.getDescription()).contains("CB 신용점수");
                });
    }

    @Test
    void rejectsScoreBelowCbMinimum() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .creditScore(250)
                .build();

        List<ConditionEvaluation> results = parser.evaluate("CB점수 271점 이상", user);

        assertThat(results)
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }

    @Test
    void supportsKcbAndNiceNotation() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .creditScore(650)
                .build();

        assertThat(parser.evaluate("KCB 점수 600점 이상", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);

        assertThat(parser.evaluate("NICE 신용점수 700점 이하", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);
    }

    @Test
    void requestsCreditScoreWhenMissing() {
        RecommendationUserDTO user = RecommendationUserDTO.builder().build();

        assertThat(parser.evaluate("CB점수 271점 이상", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NEEDS_CONFIRMATION);
    }
}
