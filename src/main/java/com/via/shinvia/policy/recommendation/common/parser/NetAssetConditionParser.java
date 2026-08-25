package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// 본인·배우자 합산 순자산 상한 조건 해석 기능
public class NetAssetConditionParser {

    private static final Pattern MAX_PATTERN = Pattern.compile(
            "(?:본인(?:\\s*및|과)?\\s*배우자(?:의)?\\s*합산\\s*)?순\\s*자산(?:\\s*(?:가액|액|금액))?[^\\d]{0,30}"
                    + "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(억원|억|천만원|백만원|만원|천원|원)\\s*이하"
    );

    public List<ConditionEvaluation> evaluate(String rawText, RecommendationUserDTO user) {
        List<ConditionEvaluation> results = new ArrayList<>();
        String text = RegionNormalizer.normalizeText(rawText);

        if (!text.contains("순자산")) {
            return results;
        }

        Matcher matcher = MAX_PATTERN.matcher(text);
        if (!matcher.find()) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.NET_ASSET,
                    "상품의 순자산 세부 기준 확인이 필요합니다."
            ));
            return results;
        }

        if (user.getHouseholdNetAssetAmount() == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.NET_ASSET,
                    "본인·배우자 합산 순자산 정보가 없어 확인이 필요합니다."
            ));
            return results;
        }

        BigDecimal maximum = toWon(matcher.group(1), matcher.group(2));
        results.add(user.getHouseholdNetAssetAmount().compareTo(maximum) <= 0
                ? ConditionEvaluation.satisfied(ConditionType.NET_ASSET, "가구 순자산 조건을 충족합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.NET_ASSET, "가구 순자산 기준을 초과합니다."));
        return results;
    }

    private BigDecimal toWon(String numberText, String unit) {
        BigDecimal number = new BigDecimal(numberText.replace(",", ""));
        BigDecimal multiplier = switch (unit) {
            case "억원", "억" -> BigDecimal.valueOf(100_000_000L);
            case "천만원" -> BigDecimal.valueOf(10_000_000L);
            case "백만원" -> BigDecimal.valueOf(1_000_000L);
            case "만원" -> BigDecimal.valueOf(10_000L);
            case "천원" -> BigDecimal.valueOf(1_000L);
            default -> BigDecimal.ONE;
        };
        return number.multiply(multiplier);
    }
}
