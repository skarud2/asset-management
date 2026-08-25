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
// 신용 조건 해석 기능
public class CreditConditionParser {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(\\d{3,4})\\s*(?:점)?\\s*[~～∼-]\\s*(\\d{3,4})\\s*점?"
    );

    private static final Pattern BETWEEN_PATTERN = Pattern.compile(
            "(\\d{3,4})\\s*점?\\s*이상.*?(\\d{3,4})\\s*점?\\s*이하"
    );

    private static final Pattern MIN_PATTERN = Pattern.compile(
            "(\\d{3,4})\\s*점?\\s*이상"
    );

    private static final Pattern MAX_PATTERN = Pattern.compile(
            "(\\d{3,4})\\s*점?\\s*이하"
    );

    private static final Pattern CB_MIN_PATTERN = Pattern.compile(
            "(?:cb|kcb|nice)\\s*(?:신용)?(?:평점|점수)?\\s*[:：]?\\s*(\\d{3,4})\\s*점?\\s*이상"
    );

    private static final Pattern CB_MAX_PATTERN = Pattern.compile(
            "(?:cb|kcb|nice)\\s*(?:신용)?(?:평점|점수)?\\s*[:：]?\\s*(\\d{3,4})\\s*점?\\s*이하"
    );

    private static final Pattern CREDIT_EXEMPT_INCOME_PATTERN = Pattern.compile(
            "연\\s*소득[^\\d]{0,15}(\\d[\\d,]*)\\s*만원\\s*이하[^.]{0,30}(?:무관|제외|적용하지)"
    );

    private static final Pattern DEBT_EXCLUSION_PATTERN = Pattern.compile(
            "(?:채무불이행자?[^.]{0,25}(?:제외|불가|제한)|(?:제외대상|지원제외)[^.]{0,80}채무불이행)"
    );

    private static final Pattern OVERDUE_EXCLUSION_PATTERN = Pattern.compile(
            "(?:현재\\s*)?연체(?:자|중)?[^.]{0,25}(?:제외|불가|제한)|(?:제외대상|지원제외)[^.]{0,80}(?:현재\\s*)?연체"
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

        evaluateDebtAndOverdue(text, user, results);

        boolean percentileRule = text.contains("하위 20%")
                || text.contains("하위 100분의 20");

        boolean externalRatingRule = text.contains("신용평가시스템")
                || text.contains("신용평가모형")
                || text.contains("일반신용정보관리규약")
                || text.contains("내부 신용등급")
                || text.contains("평가등급");

        // "연소득 3,500만원 이하는 신용평점 무관" 같은 예외를 먼저 처리한다.
        if (percentileRule && isCreditConditionExemptByIncome(text, user)) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.CREDIT,
                    "현재 소득구간에서는 별도의 신용평점 제한이 적용되지 않습니다."
            ));
            percentileRule = false;
        }

        if (percentileRule || externalRatingRule) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.CREDIT,
                    externalRatingRule
                            ? "기관의 별도 신용평가·내부등급 기준 확인이 필요합니다."
                            : "신용평점 하위 백분위 기준은 현재 점수만으로 확정할 수 없어 확인이 필요합니다."
            ));
        }

        List<ScoreRange> ranges = findAllRanges(text);
        if (!ranges.isEmpty()) {
            if (user.getCreditScore() == null) {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.CREDIT,
                        "신용점수 정보가 없어 점수 조건을 확인할 수 없습니다."
                ));
                return results;
            }

            boolean match = ranges.stream()
                    .anyMatch(range -> range.contains(user.getCreditScore()));

            results.add(match
                    ? ConditionEvaluation.satisfied(ConditionType.CREDIT, "신용점수 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.CREDIT, "신용점수 조건을 충족하지 않습니다."));
            return results;
        }

        boolean cbScoreRule = containsCbScoreKeyword(text);
        Integer min = firstNotNull(findFirst(CB_MIN_PATTERN, text), findFirst(MIN_PATTERN, text));
        Integer max = firstNotNull(findFirst(CB_MAX_PATTERN, text), findFirst(MAX_PATTERN, text));

        if (min != null || max != null) {
            if (user.getCreditScore() == null) {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.CREDIT,
                        "신용점수 정보 확인이 필요합니다."
                ));
                return results;
            }

            boolean match = (min == null || user.getCreditScore() >= min)
                    && (max == null || user.getCreditScore() <= max);

            results.add(match
                    ? ConditionEvaluation.satisfied(
                            ConditionType.CREDIT,
                            cbScoreRule ? "CB 신용점수 조건을 충족합니다." : "신용점수 조건을 충족합니다."
                    )
                    : ConditionEvaluation.notSatisfied(
                            ConditionType.CREDIT,
                            cbScoreRule ? "CB 신용점수 조건을 충족하지 않습니다." : "신용점수 조건을 충족하지 않습니다."
                    ));
        }

        // '연체정리 기업', '신용회복절차 진행기업'은 단순 현재연체 여부로 판정하면 안 된다.
        if (text.contains("연체정리") || text.contains("신용회복절차")) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.DEBT,
                    "신용회복·과거 연체정리 상태는 현재 설문만으로 확정할 수 없어 추가 확인이 필요합니다."
            ));
        }

        if (results.isEmpty() && containsCreditKeyword(text)) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.CREDIT,
                    "상품의 세부 신용 조건 확인이 필요합니다."
            ));
        }

        return results;
    }

    private void evaluateDebtAndOverdue(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        if (DEBT_EXCLUSION_PATTERN.matcher(text).find()) {
            if ("YES".equals(user.getDebtDefaultStatus())) {
                results.add(ConditionEvaluation.notSatisfied(
                        ConditionType.DEBT,
                        "채무불이행 제한조건에 해당합니다."
                ));
            } else if ("NO".equals(user.getDebtDefaultStatus())) {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.DEBT,
                        "채무불이행 제한조건에 해당하지 않습니다."
                ));
            } else {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.DEBT,
                        "채무불이행 여부 확인이 필요합니다."
                ));
            }
        }

        if (OVERDUE_EXCLUSION_PATTERN.matcher(text).find()) {
            if ("YES".equals(user.getOverdueStatus())) {
                results.add(ConditionEvaluation.notSatisfied(
                        ConditionType.DEBT,
                        "현재 연체 상태로 이용이 제한되는 상품입니다."
                ));
            } else if ("NO".equals(user.getOverdueStatus())) {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.DEBT,
                        "현재 연체 제한조건에 해당하지 않습니다."
                ));
            } else {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.DEBT,
                        "현재 연체 여부 확인이 필요합니다."
                ));
            }
        }
    }

    private boolean isCreditConditionExemptByIncome(
            String text,
            RecommendationUserDTO user
    ) {
        if (user.getAnnualIncome() == null) {
            return false;
        }

        Matcher matcher = CREDIT_EXEMPT_INCOME_PATTERN.matcher(text);
        if (matcher.find()) {
            BigDecimal threshold = new BigDecimal(matcher.group(1).replace(",", ""))
                    .multiply(BigDecimal.valueOf(10_000L));
            return user.getAnnualIncome().compareTo(threshold) <= 0;
        }

        // income 문구까지 함께 전달된 경우:
        // "3500만원 이하 또는 4500만원 이하이면서 신용평점 하위20%"
        if (text.contains("또는") && (text.contains("연소득") || text.contains("연 소득"))) {
            Matcher amountMatcher = Pattern.compile("(\\d[\\d,]*)\\s*만원\\s*이하").matcher(text);
            BigDecimal smallest = null;

            while (amountMatcher.find()) {
                BigDecimal amount = new BigDecimal(amountMatcher.group(1).replace(",", ""))
                        .multiply(BigDecimal.valueOf(10_000L));
                if (smallest == null || amount.compareTo(smallest) < 0) {
                    smallest = amount;
                }
            }

            return smallest != null && user.getAnnualIncome().compareTo(smallest) <= 0;
        }

        return false;
    }

    private List<ScoreRange> findAllRanges(String text) {
        List<ScoreRange> ranges = new ArrayList<>();

        Matcher between = BETWEEN_PATTERN.matcher(text);
        while (between.find()) {
            ranges.add(new ScoreRange(
                    Integer.parseInt(between.group(1)),
                    Integer.parseInt(between.group(2))
            ));
        }

        Matcher range = RANGE_PATTERN.matcher(text);
        while (range.find()) {
            ScoreRange scoreRange = new ScoreRange(
                    Integer.parseInt(range.group(1)),
                    Integer.parseInt(range.group(2))
            );
            if (!ranges.contains(scoreRange)) {
                ranges.add(scoreRange);
            }
        }

        return ranges;
    }

    private Integer findFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Integer firstNotNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private boolean containsCbScoreKeyword(String text) {
        return text.contains("cb점수")
                || text.contains("cb 점수")
                || text.contains("cb평점")
                || text.contains("kcb")
                || text.contains("nice");
    }

    private boolean containsCreditKeyword(String text) {
        return text.contains("신용")
                || text.contains("평점")
                || containsCbScoreKeyword(text)
                || text.contains("연체")
                || text.contains("채무");
    }

    private boolean isNoCondition(String text) {
        return text.isBlank()
                || "없음".equals(text)
                || "0".equals(text)
                || "null".equals(text);
    }

    private record ScoreRange(int min, int max) {
        private boolean contains(int score) {
            return score >= min && score <= max;
        }
    }
}
