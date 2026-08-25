package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// 소득 조건 해석 기능
public class IncomeConditionParser {

    private static final String AMOUNT = "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(억원|천만원|백만원|만원|천원|원)";

    private static final Pattern MONTHLY_RANGE_PATTERN = Pattern.compile(
            "(?:월\\s*(?:평균\\s*)?(?:소득|급여)(?:액|총액)?[^\\d]{0,20})" + AMOUNT
                    + "\\s*[~～∼-]\\s*" + AMOUNT
    );

    private static final Pattern MONTHLY_MAX_PATTERN = Pattern.compile(
            "(?:월\\s*(?:평균\\s*)?(?:소득|급여)(?:액|총액)?[^\\d]{0,20})" + AMOUNT + "\\s*이하"
    );

    private static final Pattern TOTAL_SALARY_MAX_PATTERN = Pattern.compile(
            "(?:총\\s*급여(?:액)?|근로소득금액)[^\\d]{0,20}" + AMOUNT + "\\s*이하"
    );

    private static final Pattern COMPREHENSIVE_INCOME_MAX_PATTERN = Pattern.compile(
            "종합소득(?:금액)?[^\\d]{0,20}" + AMOUNT + "\\s*이하"
    );

    private static final Pattern ANNUAL_MAX_PATTERN = Pattern.compile(
            "(?:개인\\s*)?(?:연\\s*소득|연소득|연간소득)[^\\d]{0,20}" + AMOUNT + "\\s*이하"
    );

    private static final Pattern SIMPLE_MAX_PATTERN = Pattern.compile(
            AMOUNT + "\\s*이하"
    );

    public List<ConditionEvaluation> evaluate(
            String rawText,
            RecommendationUserDTO user
    ) {
        List<ConditionEvaluation> results = new ArrayList<>();
        String text = RegionNormalizer.normalizeText(rawText);

        if (isNoCondition(text) || isOnlyWelfareText(text)) {
            return results;
        }

        // 순자산은 소득이 아니므로 전용 파서에서 판정한다.
        if (text.contains("순자산") && !containsIncomeKeyword(text)) {
            return results;
        }

        // "3,500만원 이하 또는 4,500만원 이하이면서 신용 하위20%"처럼
        // 소득과 신용이 OR로 묶인 조건은 단순 최대소득 하나로 잘라내면 오판한다.
        if (evaluateConditionalIncomeCredit(text, user, results)) {
            return results;
        }

        boolean householdIncome = text.contains("부부합산")
                || text.contains("배우자")
                || text.contains("가구소득")
                || text.contains("가구 소득")
                || text.contains("부양의무자");

        boolean medianIncome = text.contains("중위소득")
                || text.contains("최저생계비")
                || text.contains("건강보험료")
                || text.contains("소득인정액");

        boolean businessRevenue = text.contains("연매출")
                || text.contains("매출액");

        BigDecimal evaluationIncome = householdIncome
                ? resolveHouseholdAnnualIncome(user)
                : user.getAnnualIncome();

        if (evaluationIncome == null && containsNumericIncomeCondition(text)) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    householdIncome
                            ? "가구 합산 연소득 정보가 없어 소득 조건을 확인할 수 없습니다."
                            : "연소득 정보가 없어 소득 조건을 확인할 수 없습니다."
            ));
            return results;
        }

        // 비정규직은 소득요건을 적용하지 않는다는 예외가 명시된 경우
        if (isIncomeExemptForNonRegularWorker(text, user)) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.INCOME,
                    "비정규직 근로자에게는 해당 소득요건이 적용되지 않습니다."
            ));
            return results;
        }

        BigDecimal typeSpecificMax = householdIncome
                ? null
                : findTypeSpecificIncomeMax(text, user.getIncomeType());
        if (typeSpecificMax != null && user.getAnnualIncome() != null) {
            results.add(compareMaxAnnualIncome(user.getAnnualIncome(), typeSpecificMax));
        }

        boolean householdIncomeEvaluated = false;
        if (!medianIncome && !businessRevenue && typeSpecificMax == null) {
            BigDecimal annualMax = findAmount(ANNUAL_MAX_PATTERN, text);
            if (annualMax == null && !hasMonthlyIncomeCondition(text)) {
                // income 컬럼이 "3,500만원 이하"처럼 금액만 내려오는 경우
                annualMax = findAmount(SIMPLE_MAX_PATTERN, text);
            }

            if (annualMax != null && evaluationIncome != null) {
                results.add(compareMaxIncome(evaluationIncome, annualMax, householdIncome));
                householdIncomeEvaluated = householdIncome;
            }
        }

        AmountRange monthlyRange = findMonthlyRange(text);
        if (monthlyRange != null && user.getAnnualIncome() != null) {
            BigDecimal monthlyIncome = monthlyIncome(user.getAnnualIncome());

            boolean match = monthlyIncome.compareTo(monthlyRange.min()) >= 0
                    && monthlyIncome.compareTo(monthlyRange.max()) <= 0;

            results.add(match
                    ? ConditionEvaluation.satisfied(ConditionType.INCOME, "월 소득 범위 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.INCOME, "월 소득 범위 조건을 충족하지 않습니다."));
        } else {
            BigDecimal monthlyMax = findAmount(MONTHLY_MAX_PATTERN, text);
            if (monthlyMax != null && user.getAnnualIncome() != null) {
                BigDecimal monthlyIncome = monthlyIncome(user.getAnnualIncome());

                results.add(monthlyIncome.compareTo(monthlyMax) <= 0
                        ? ConditionEvaluation.satisfied(ConditionType.INCOME, "월 소득 상한 조건을 충족합니다.")
                        : ConditionEvaluation.notSatisfied(ConditionType.INCOME, "월 소득 상한 기준을 초과합니다."));
            }
        }

        if (householdIncome && !householdIncomeEvaluated) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "상품의 부부·가구 합산소득 세부 기준은 추가 확인이 필요합니다."
            ));
        }

        if (medianIncome) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "기준중위소득·건강보험료 기준은 가구 전체 정보 확인이 필요합니다."
            ));
        }

        if (businessRevenue) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "매출액 기준은 연소득과 다른 값이므로 사업 매출 확인이 필요합니다."
            ));
        }

        if (text.contains("소득증빙") || text.contains("원천징수")) {
            results.add(evaluateIncomeProof(user));
        }

        if (results.isEmpty() && containsIncomeKeyword(text)) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "상품의 세부 소득 조건 확인이 필요합니다."
            ));
        }

        return results;
    }

    private boolean evaluateConditionalIncomeCredit(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        boolean conditionalCredit = text.contains("또는")
                && (text.contains("신용평점") || text.contains("개인신용") || text.contains("신용점수"))
                && (text.contains("연소득") || text.contains("연 소득"));

        if (!conditionalCredit) {
            return false;
        }

        List<BigDecimal> thresholds = findAllMaxAmounts(text);
        if (thresholds.size() < 2) {
            return false;
        }

        if (user.getAnnualIncome() == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "연소득 정보가 없어 소득·신용 결합조건을 확인할 수 없습니다."
            ));
            return true;
        }

        thresholds.sort(Comparator.naturalOrder());
        BigDecimal unconditionalMax = thresholds.get(0);
        BigDecimal conditionalMax = thresholds.get(thresholds.size() - 1);

        if (user.getAnnualIncome().compareTo(unconditionalMax) <= 0) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.INCOME,
                    "신용점수 추가조건이 없는 소득구간을 충족합니다."
            ));
        } else if (user.getAnnualIncome().compareTo(conditionalMax) <= 0) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "상위 소득구간은 별도의 신용평점 조건을 함께 확인해야 합니다."
            ));
        } else {
            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.INCOME,
                    "상품의 최대 소득기준을 초과합니다."
            ));
        }

        return true;
    }

    private List<BigDecimal> findAllMaxAmounts(String text) {
        List<BigDecimal> amounts = new ArrayList<>();
        Matcher matcher = SIMPLE_MAX_PATTERN.matcher(text);

        while (matcher.find()) {
            amounts.add(toWon(matcher.group(1), matcher.group(2)));
        }

        return amounts;
    }

    private boolean isIncomeExemptForNonRegularWorker(
            String text,
            RecommendationUserDTO user
    ) {
        if (!(text.contains("비정규직")
                && text.contains("소득요건")
                && (text.contains("적용하지") || text.contains("미적용")))) {
            return false;
        }

        return switch (safe(user.getEmploymentStatus())) {
            case "CONTRACT", "TEMPORARY", "DISPATCH", "DAILY" -> true;
            default -> false;
        };
    }

    private BigDecimal findTypeSpecificIncomeMax(String text, String incomeType) {
        BigDecimal totalSalaryMax = findAmount(TOTAL_SALARY_MAX_PATTERN, text);
        BigDecimal comprehensiveMax = findAmount(COMPREHENSIVE_INCOME_MAX_PATTERN, text);

        if (totalSalaryMax == null && comprehensiveMax == null) {
            return null;
        }

        if ("EMPLOYMENT".equals(incomeType) && totalSalaryMax != null) {
            return totalSalaryMax;
        }

        if (!"EMPLOYMENT".equals(incomeType) && comprehensiveMax != null) {
            return comprehensiveMax;
        }

        return totalSalaryMax != null ? totalSalaryMax : comprehensiveMax;
    }

    private ConditionEvaluation compareMaxAnnualIncome(
            BigDecimal annualIncome,
            BigDecimal maxIncome
    ) {
        return annualIncome.compareTo(maxIncome) <= 0
                ? ConditionEvaluation.satisfied(ConditionType.INCOME, "연소득 조건을 충족합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.INCOME, "연소득 기준을 초과합니다.");
    }

    private ConditionEvaluation compareMaxIncome(
            BigDecimal income,
            BigDecimal maxIncome,
            boolean householdIncome
    ) {
        if (!householdIncome) {
            return compareMaxAnnualIncome(income, maxIncome);
        }

        return income.compareTo(maxIncome) <= 0
                ? ConditionEvaluation.satisfied(ConditionType.INCOME, "가구 합산 연소득 조건을 충족합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.INCOME, "가구 합산 연소득 기준을 초과합니다.");
    }

    private BigDecimal resolveHouseholdAnnualIncome(RecommendationUserDTO user) {
        if (user.getHouseholdAnnualIncome() != null) {
            return user.getHouseholdAnnualIncome();
        }

        // 미혼 사용자는 배우자 소득이 없으므로 본인 연소득을 가구 합산소득으로 사용할 수 있다.
        if ("SINGLE".equals(user.getMaritalStatus())) {
            return user.getAnnualIncome();
        }

        return null;
    }

    private ConditionEvaluation evaluateIncomeProof(RecommendationUserDTO user) {
        if ("YES".equals(user.getIncomeVerifiable())) {
            return ConditionEvaluation.satisfied(
                    ConditionType.INCOME_PROOF,
                    "소득증빙 가능 조건을 충족합니다."
            );
        }

        if ("NO".equals(user.getIncomeVerifiable())) {
            return ConditionEvaluation.notSatisfied(
                    ConditionType.INCOME_PROOF,
                    "소득증빙이 필요한 상품입니다."
            );
        }

        return ConditionEvaluation.needsConfirmation(
                ConditionType.INCOME_PROOF,
                "소득증빙 가능 여부 확인이 필요합니다."
        );
    }

    private AmountRange findMonthlyRange(String text) {
        Matcher matcher = MONTHLY_RANGE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        BigDecimal min = toWon(matcher.group(1), matcher.group(2));
        BigDecimal max = toWon(matcher.group(3), matcher.group(4));
        return new AmountRange(min, max);
    }

    private BigDecimal findAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        return toWon(matcher.group(1), matcher.group(2));
    }

    private BigDecimal monthlyIncome(BigDecimal annualIncome) {
        return annualIncome.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal toWon(String numberText, String unit) {
        BigDecimal number = new BigDecimal(numberText.replace(",", ""));

        BigDecimal multiplier = switch (unit) {
            case "억원" -> BigDecimal.valueOf(100_000_000L);
            case "천만원" -> BigDecimal.valueOf(10_000_000L);
            case "백만원" -> BigDecimal.valueOf(1_000_000L);
            case "만원" -> BigDecimal.valueOf(10_000L);
            case "천원" -> BigDecimal.valueOf(1_000L);
            default -> BigDecimal.ONE;
        };

        return number.multiply(multiplier);
    }

    private boolean containsNumericIncomeCondition(String text) {
        return containsIncomeKeyword(text) && text.matches(".*\\d.*");
    }

    private boolean hasMonthlyIncomeCondition(String text) {
        return text.contains("월소득")
                || text.contains("월 소득")
                || text.contains("월평균소득")
                || text.contains("월 평균 소득")
                || text.contains("월급여")
                || text.contains("월 급여");
    }

    private boolean containsIncomeKeyword(String text) {
        return text.contains("소득")
                || text.contains("급여")
                || text.contains("매출")
                || text.contains("생계비")
                || text.contains("건강보험료");
    }

    private boolean isOnlyWelfareText(String text) {
        boolean welfare = text.contains("기초생활")
                || text.contains("차상위")
                || text.contains("한부모");

        return welfare
                && !text.matches(".*\\d.*")
                && !text.contains("소득증빙")
                && !text.contains("원천징수");
    }

    private boolean isNoCondition(String text) {
        return text.isBlank()
                || "없음".equals(text)
                || "0".equals(text)
                || "null".equals(text);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record AmountRange(BigDecimal min, BigDecimal max) {
    }
}
