package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
// 복지·취약계층 조건 해석 기능
public class WelfareConditionParser {

    public List<ConditionEvaluation> evaluate(
            String rawText,
            RecommendationUserDTO user
    ) {
        List<ConditionEvaluation> results = new ArrayList<>();
        String text = RegionNormalizer.normalizeText(rawText);

        if (text.isBlank()) {
            return results;
        }

        if (text.contains("수급자가 아닌")
                || text.contains("수급자 제외")
                || text.contains("수급자는 제외")) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.WELFARE,
                    "특정 급여 수급 제외조건은 현재 설문만으로 세부 구분이 어려워 확인이 필요합니다."
            ));
            return results;
        }

        boolean basic = text.contains("기초생활") || text.contains("기초생활수급") || text.contains("생계급여수급");
        boolean near = text.contains("차상위");
        boolean singleParent = text.contains("한부모");
        boolean disabled = text.contains("장애");
        boolean severeDisabled = text.contains("중증장애") || text.contains("장애 정도가 심한");
        boolean disabilityBenefit = text.contains("장애인연금")
                || text.contains("장애수당")
                || text.contains("장애아동수당");
        boolean selfReliance = text.contains("자립준비");
        boolean multicultural = text.contains("다문화") || text.contains("결혼이민");
        boolean northKoreanDefector = text.contains("북한이탈") || text.contains("새터민");
        boolean childHeadedHousehold = text.contains("소년소녀");
        boolean earnedIncomeTaxCredit = text.contains("근로장려금");
        boolean basicPension = text.contains("기초연금");
        boolean jeonseFraudVictim = text.contains("전세피해") || text.contains("전세사기 피해");

        boolean hasKnownCondition = basic || near || singleParent || disabled || selfReliance || multicultural
                || northKoreanDefector || childHeadedHousehold || earnedIncomeTaxCredit || basicPension
                || jeonseFraudVictim;
        boolean hasUnknownAlternative = containsUnknownAlternative(text);

        if (!hasKnownCondition && !hasUnknownAlternative) {
            return results;
        }

        if (basic && Boolean.TRUE.equals(user.getBasicLivelihoodRecipient())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "기초생활수급자 조건에 해당합니다."
            ));
            return results;
        }

        if (near && Boolean.TRUE.equals(user.getNearPoverty())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "차상위계층 조건에 해당합니다."
            ));
            return results;
        }

        if (singleParent && Boolean.TRUE.equals(user.getSingleParentHousehold())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "한부모가구 조건에 해당합니다."
            ));
            return results;
        }

        if (selfReliance && Boolean.TRUE.equals(user.getSelfRelianceYouth())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "자립준비청년 조건에 해당합니다."
            ));
            return results;
        }

        if (multicultural && Boolean.TRUE.equals(user.getMulticulturalHousehold())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "다문화가구 조건에 해당합니다."
            ));
            return results;
        }

        if (disabilityBenefit && Boolean.TRUE.equals(user.getDisabilityBenefitRecipient())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "장애인연금·수당 수급자 조건에 해당합니다."
            ));
            return results;
        }

        if (disabled && !disabilityBenefit && Boolean.TRUE.equals(user.getDisabled())) {
            if (severeDisabled) {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.WELFARE,
                        "장애 여부는 확인되었으나 중증·장애정도 기준은 추가 확인이 필요합니다."
                ));
            } else {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.WELFARE,
                        "장애인 대상 조건에 해당합니다."
                ));
            }
            return results;
        }

        if (northKoreanDefector && Boolean.TRUE.equals(user.getNorthKoreanDefector())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "북한이탈주민 대상 조건에 해당합니다."
            ));
            return results;
        }

        if (childHeadedHousehold && Boolean.TRUE.equals(user.getChildHeadedHousehold())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "소년소녀가장 대상 조건에 해당합니다."
            ));
            return results;
        }

        if (earnedIncomeTaxCredit && Boolean.TRUE.equals(user.getEarnedIncomeTaxCreditRecipient())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "근로장려금 수급자 조건에 해당합니다."
            ));
            return results;
        }

        if (basicPension && Boolean.TRUE.equals(user.getBasicPensionRecipient())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "기초연금 수급자 조건에 해당합니다."
            ));
            return results;
        }

        if (jeonseFraudVictim && Boolean.TRUE.equals(user.getJeonseFraudVictim())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.WELFARE,
                    "전세사기 피해자 대상 조건에 해당합니다."
            ));
            return results;
        }

        if (hasUnknownAlternative) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.WELFARE,
                    hasKnownCondition
                            ? "설문에서 확인한 복지 자격에는 해당하지 않지만, 설문으로 확인하지 않는 추가 대상자 자격이 함께 있어 별도 확인이 필요합니다."
                            : "설문으로 확인하지 않는 특수 대상자 자격이 있어 별도 확인이 필요합니다."
            ));
            return results;
        }

        results.add(ConditionEvaluation.notSatisfied(
                ConditionType.WELFARE,
                "상품이 요구하는 복지·취약계층 조건에 해당하지 않습니다."
        ));

        return results;
    }

    private boolean containsUnknownAlternative(String text) {
        return text.contains("보호아동")
                || text.contains("가정위탁")
                || text.contains("시설보호")
                || text.contains("주거취약")
                || text.contains("문화예술인")
                || text.contains("어업인")
                || text.contains("귀어인")
                || text.contains("플랫폼노동")
                || text.contains("이주배경청년")
                || text.contains("채무조정");
    }
}
