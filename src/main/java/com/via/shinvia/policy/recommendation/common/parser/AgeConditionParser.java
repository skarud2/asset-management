package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// 연령 조건 해석 기능
public class AgeConditionParser {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세?\\s*(?:이상\\s*)?[~～∼-]\\s*(?:만\\s*)?(\\d{1,3})\\s*세?"
    );

    private static final Pattern BETWEEN_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*이상.*?(?:만\\s*)?(\\d{1,3})\\s*세\\s*이하"
    );

    private static final Pattern MIN_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*이상"
    );

    private static final Pattern MAX_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*이하"
    );

    private static final Pattern LESS_THAN_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*미만"
    );

    private static final Pattern CUTOFF_PATTERN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세(?:까지(?:만)?|로\\s*한정)"
    );

    private static final Pattern UNDER_EXCLUSION_PATTERN = Pattern.compile(
            "(\\d{1,3})\\s*세\\s*미만[^.]{0,30}(?:제외|불가|제한)"
    );

    private static final Pattern OVER_EXCLUSION_PATTERN = Pattern.compile(
            "(\\d{1,3})\\s*세\\s*이상[^.]{0,30}(?:제외|불가|제한)"
    );

    private static final Pattern AGE_KEYWORD_PATTERN = Pattern.compile(
            "(?:만\\s*)?\\d{1,3}\\s*세|미성년|성년|연령|나이"
    );

    public List<ConditionEvaluation> evaluate(
            String rawText,
            RecommendationUserDTO user
    ) {
        List<ConditionEvaluation> results = new ArrayList<>();
        String text = RegionNormalizer.normalizeText(rawText);

        if (isNoCondition(text)) {
            return results;
        }

        // '세대주'의 '세'처럼 연령과 무관한 글자가 포함된 문장은 무시한다.
        if (!AGE_KEYWORD_PATTERN.matcher(text).find()) {
            return results;
        }

        Integer age = user.getAge();
        if (age == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.AGE,
                    "회원의 연령 정보를 확인할 수 없습니다."
            ));
            return results;
        }

        // '65세 이상(차상위계층만 해당)'은 대상자 분기에 따라 적용 여부가 달라진다.
        if (text.contains("차상위") && text.contains("65세 이상") && text.contains("해당")) {
            if (!Boolean.TRUE.equals(user.getNearPoverty())) {
                return results;
            }

            if (age >= 65) {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.AGE,
                        "차상위계층 연령 조건을 충족합니다."
                ));
            } else {
                // 기초생활수급자 등 다른 가입대상 분기가 함께 있을 수 있으므로 단정 탈락시키지 않는다.
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.AGE,
                        "차상위계층에 적용되는 고령자 연령조건의 적용 여부를 추가 확인해야 합니다."
                ));
            }
            return results;
        }

        boolean militaryException = text.contains("병역")
                || text.contains("군 경력")
                || text.contains("군경력");

        // 19~34세, 19세 이상 34세 이하처럼 명확한 범위
        AgeRange explicitRange = findExplicitRange(text);
        if (explicitRange != null) {
            if (matches(age, explicitRange)) {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.AGE,
                        "연령 조건을 충족합니다."
                ));
                return results;
            }

            if (militaryException && explicitRange.maxAge() != null) {
                Integer exceptionMax = findLargestAge(text);
                if (exceptionMax != null
                        && age > explicitRange.maxAge()
                        && age <= exceptionMax) {
                    results.add(ConditionEvaluation.needsConfirmation(
                            ConditionType.AGE,
                            "기본 연령 기준은 초과하지만 병역이행에 따른 연령 연장 적용 여부 확인이 필요합니다."
                    ));
                    return results;
                }
            }

            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.AGE,
                    "연령 조건을 충족하지 않습니다."
            ));
            return results;
        }

        // '18세 미만 제외', '70세 이상 제외'는 일반 '이상/이하'보다 먼저 본다.
        Matcher underExclusion = UNDER_EXCLUSION_PATTERN.matcher(text);
        Matcher overExclusion = OVER_EXCLUSION_PATTERN.matcher(text);

        boolean hasUnderExclusion = underExclusion.find();
        boolean hasOverExclusion = overExclusion.find();

        if (hasUnderExclusion || hasOverExclusion) {
            boolean match = true;

            if (hasUnderExclusion) {
                int excludedBelow = Integer.parseInt(underExclusion.group(1));
                match = age >= excludedBelow;
            }

            if (match && hasOverExclusion) {
                int excludedFrom = Integer.parseInt(overExclusion.group(1));
                match = age < excludedFrom;
            }

            results.add(match
                    ? ConditionEvaluation.satisfied(ConditionType.AGE, "연령 제한조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.AGE, "연령 제한조건을 충족하지 않습니다."));
            return results;
        }

        Matcher lessThan = LESS_THAN_PATTERN.matcher(text);
        if (lessThan.find()) {
            int maxExclusive = Integer.parseInt(lessThan.group(1));
            results.add(age < maxExclusive
                    ? ConditionEvaluation.satisfied(ConditionType.AGE, "연령 상한 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.AGE, "연령 상한 조건을 충족하지 않습니다."));
            return results;
        }

        Matcher cutoff = CUTOFF_PATTERN.matcher(text);
        if (cutoff.find()) {
            int maxAge = Integer.parseInt(cutoff.group(1));
            results.add(age <= maxAge
                    ? ConditionEvaluation.satisfied(ConditionType.AGE, "연령 상한 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.AGE, "연령 상한 조건을 충족하지 않습니다."));
            return results;
        }

        AgeRange oneSided = findOneSidedRange(text);
        if (oneSided != null) {
            if (matches(age, oneSided)) {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.AGE,
                        "연령 조건을 충족합니다."
                ));
            } else if (militaryException && oneSided.maxAge() != null) {
                Integer exceptionMax = findLargestAge(text);
                if (exceptionMax != null && age <= exceptionMax) {
                    results.add(ConditionEvaluation.needsConfirmation(
                            ConditionType.AGE,
                            "병역이행에 따른 연령 연장 적용 여부 확인이 필요합니다."
                    ));
                } else {
                    results.add(ConditionEvaluation.notSatisfied(
                            ConditionType.AGE,
                            "연령 조건을 충족하지 않습니다."
                    ));
                }
            } else {
                results.add(ConditionEvaluation.notSatisfied(
                        ConditionType.AGE,
                        "연령 조건을 충족하지 않습니다."
                ));
            }
            return results;
        }

        if (text.contains("미성년자 제외") || text.contains("민법상 성년")) {
            results.add(age >= 19
                    ? ConditionEvaluation.satisfied(ConditionType.AGE, "성년자 연령 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.AGE, "성년자만 이용할 수 있는 상품입니다."));
            return results;
        }

        if (text.contains("나이제한 없음") || text.contains("연령제한 없음")) {
            return results;
        }

        results.add(ConditionEvaluation.needsConfirmation(
                ConditionType.AGE,
                "상품의 세부 연령 조건 확인이 필요합니다."
        ));

        return results;
    }

    private AgeRange findExplicitRange(String text) {
        Matcher between = BETWEEN_PATTERN.matcher(text);
        if (between.find()) {
            return new AgeRange(
                    Integer.parseInt(between.group(1)),
                    Integer.parseInt(between.group(2))
            );
        }

        Matcher range = RANGE_PATTERN.matcher(text);
        if (range.find()) {
            return new AgeRange(
                    Integer.parseInt(range.group(1)),
                    Integer.parseInt(range.group(2))
            );
        }

        return null;
    }

    private AgeRange findOneSidedRange(String text) {
        Matcher min = MIN_PATTERN.matcher(text);
        Matcher max = MAX_PATTERN.matcher(text);

        Integer minAge = min.find() ? Integer.parseInt(min.group(1)) : null;
        Integer maxAge = max.find() ? Integer.parseInt(max.group(1)) : null;

        if (minAge == null && (text.contains("민법상 성년") || text.contains("성년자"))) {
            minAge = 19;
        }

        if (minAge == null && maxAge == null) {
            return null;
        }

        return new AgeRange(minAge, maxAge);
    }

    private Integer findLargestAge(String text) {
        Matcher matcher = Pattern.compile("(\\d{1,3})\\s*세").matcher(text);
        Integer largest = null;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (largest == null || value > largest) {
                largest = value;
            }
        }

        return largest;
    }

    private boolean matches(Integer age, AgeRange range) {
        if (range.minAge() != null && age < range.minAge()) {
            return false;
        }
        return range.maxAge() == null || age <= range.maxAge();
    }

    private boolean isNoCondition(String text) {
        return text.isBlank()
                || "없음".equals(text)
                || "0".equals(text)
                || "null".equals(text);
    }

    private record AgeRange(Integer minAge, Integer maxAge) {
    }
}
