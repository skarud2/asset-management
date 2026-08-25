package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmploymentConditionParserTest {

    private final EmploymentConditionParser parser = new EmploymentConditionParser();

    @Test
    void rejectsNonBusinessUserForBusinessOnlyProduct() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .employmentStatus("REGULAR")
                .build();

        List<ConditionEvaluation> results = parser.evaluate("소상공인 사업자 대상", user);

        assertThat(results).anyMatch(this::isEmploymentFailure);
    }

    @Test
    void acceptsBusinessUserForBusinessOnlyProduct() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .employmentStatus("BUSINESS")
                .build();

        List<ConditionEvaluation> results = parser.evaluate("소상공인 사업자 대상", user);

        assertThat(results).anyMatch(result -> result.getType() == ConditionType.EMPLOYMENT
                && result.getStatus() == ConditionStatus.SATISFIED);
    }

    @Test
    void rejectsWorkerForStudentOnlyProduct() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .employmentStatus("REGULAR")
                .build();

        List<ConditionEvaluation> results = parser.evaluate("대학생 대상", user);

        assertThat(results).anyMatch(this::isEmploymentFailure);
    }

    @Test
    void requestsConfirmationForUniversityStudentDetails() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .employmentStatus("STUDENT")
                .build();

        List<ConditionEvaluation> results = parser.evaluate("대학생 및 재학생 대상", user);

        assertThat(results).anyMatch(result -> result.getType() == ConditionType.EMPLOYMENT
                && result.getStatus() == ConditionStatus.NEEDS_CONFIRMATION);
    }

    private boolean isEmploymentFailure(ConditionEvaluation result) {
        return result.getType() == ConditionType.EMPLOYMENT
                && result.getStatus() == ConditionStatus.NOT_SATISFIED;
    }
}
