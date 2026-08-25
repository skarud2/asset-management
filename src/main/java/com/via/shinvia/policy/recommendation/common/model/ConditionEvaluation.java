package com.via.shinvia.policy.recommendation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 개별 추천 조건 판정 결과
public class ConditionEvaluation {
    private ConditionType type;
    private ConditionStatus status;
    private String description;

    public static ConditionEvaluation satisfied(
            ConditionType type,
            String description
    ) {
        return new ConditionEvaluation(
                type,
                ConditionStatus.SATISFIED,
                description
        );
    }

    public static ConditionEvaluation needsConfirmation(
            ConditionType type,
            String description
    ) {
        return new ConditionEvaluation(
                type,
                ConditionStatus.NEEDS_CONFIRMATION,
                description
        );
    }

    public static ConditionEvaluation notSatisfied(
            ConditionType type,
            String description
    ) {
        return new ConditionEvaluation(
                type,
                ConditionStatus.NOT_SATISFIED,
                description
        );
    }
}
