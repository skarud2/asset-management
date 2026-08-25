package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NetAssetConditionParserTest {

    private final NetAssetConditionParser parser = new NetAssetConditionParser();

    @Test
    void evaluatesDecimalEokMaximum() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdNetAssetAmount(BigDecimal.valueOf(500_000_000L))
                .build();

        List<ConditionEvaluation> results = parser.evaluate(
                "본인 및 배우자 합산 순자산 가액 5.06억 이하",
                user
        );

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getType()).isEqualTo(ConditionType.NET_ASSET);
                    assertThat(result.getStatus()).isEqualTo(ConditionStatus.SATISFIED);
                });
    }

    @Test
    void rejectsNetAssetAboveMaximum() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdNetAssetAmount(BigDecimal.valueOf(400_000_000L))
                .build();

        assertThat(parser.evaluate("순자산가액 3.45억원 이하", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NOT_SATISFIED);
    }

    @Test
    void requestsConfirmationWhenNetAssetWasNotEntered() {
        RecommendationUserDTO user = RecommendationUserDTO.builder().build();

        assertThat(parser.evaluate("순자산가액 3.45억원 이하", user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.NEEDS_CONFIRMATION);
    }

    @Test
    void extractsNetAssetFromCombinedEligibilityText() {
        RecommendationUserDTO user = RecommendationUserDTO.builder()
                .householdNetAssetAmount(BigDecimal.valueOf(300_000_000L))
                .build();
        String condition = "민법상 성년, 대한민국 국민, 접수일 현재 세대주, "
                + "CB점수 350점 이상, 본인 및 배우자 합산 순자산 가액 5.06억 이하";

        assertThat(parser.evaluate(condition, user))
                .singleElement()
                .extracting(ConditionEvaluation::getStatus)
                .isEqualTo(ConditionStatus.SATISFIED);
    }
}
