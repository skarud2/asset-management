package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgeConditionParserTest {

    private final AgeConditionParser parser = new AgeConditionParser();

    @Test
    void appliesCivilAdultMinimumWithAgeMaximum() {
        String condition = "대출신청일 기준 만 34세 이하의 민법상 성년";

        RecommendationUserDTO adult = RecommendationUserDTO.builder().age(25).build();
        assertThat(parser.evaluate(condition, adult))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);

        RecommendationUserDTO minor = RecommendationUserDTO.builder().age(18).build();
        assertThat(parser.evaluate(condition, minor))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }
}
