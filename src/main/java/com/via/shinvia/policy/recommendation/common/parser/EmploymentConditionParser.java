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
// 고용형태·재직/사업기간·소득증빙 조건 해석 기능
public class EmploymentConditionParser {

    private static final Pattern PERIOD_MONTHS_A = Pattern.compile(
            "(?:재직|근로|사업|업력|사업자등록)[^\\d]{0,20}(\\d+)\\s*개월\\s*이상"
    );

    private static final Pattern PERIOD_MONTHS_B = Pattern.compile(
            "(\\d+)\\s*개월\\s*이상[^.]{0,30}(?:재직|근로|사업|영업|업력)"
    );

    private static final Pattern PERIOD_YEARS_A = Pattern.compile(
            "(?:재직|근로|사업|업력|사업자등록)[^\\d]{0,20}(\\d+)\\s*년\\s*이상"
    );

    private static final Pattern PERIOD_YEARS_B = Pattern.compile(
            "(\\d+)\\s*년\\s*이상[^.]{0,30}(?:재직|근로|사업|영업|업력)"
    );

    private static final Pattern CONTRACT_EXCLUSION = Pattern.compile(
            "(\\d+)\\s*년\\s*미만(?:의)?\\s*계약직.*?(?:불가|제외|제한)"
    );

    private static final Pattern BUSINESS_EXCLUSION = Pattern.compile(
            "(?:개인\\s*)?사업자[^.]{0,40}(?:제외|불가|지원대상(?:이|은)?\\s*아님|해당하지)"
    );

    public List<ConditionEvaluation> evaluate(
            String rawText,
            RecommendationUserDTO user
    ) {
        List<ConditionEvaluation> results = new ArrayList<>();
        String text = RegionNormalizer.normalizeText(rawText);

        if (text.isBlank()) {
            return results;
        }

        evaluateIncomeExistence(text, user, results);
        evaluateIncomeProof(text, user, results);
        evaluateContractExclusion(text, user, results);
        evaluateEmploymentType(text, user, results);
        evaluateEmploymentPeriod(text, user, results);

        if (text.contains("고용보험") || text.contains("4대보험") || text.contains("산재보험")) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.EMPLOYMENT,
                    "고용·사회보험 가입 여부는 추가 확인이 필요합니다."
            ));
        }

        return results;
    }

    private void evaluateIncomeExistence(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        if (!text.contains("소득이 없") || !(text.contains("불가") || text.contains("제외") || text.contains("제한"))) {
            return;
        }

        if (Boolean.TRUE.equals(user.getHasIncome())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.INCOME,
                    "소득 보유 조건을 충족합니다."
            ));
        } else if (Boolean.FALSE.equals(user.getHasIncome())) {
            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.INCOME,
                    "현재 소득이 없는 경우 이용할 수 없는 상품입니다."
            ));
        } else {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME,
                    "현재 소득 여부 확인이 필요합니다."
            ));
        }
    }

    private void evaluateIncomeProof(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        boolean requiresProof = text.contains("소득증빙")
                || text.contains("원천징수영수증")
                || text.contains("소득 서류")
                || text.contains("소득서류");

        if (!requiresProof) {
            return;
        }

        if ("YES".equals(user.getIncomeVerifiable())) {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.INCOME_PROOF,
                    "소득증빙 가능 조건을 충족합니다."
            ));
        } else if ("NO".equals(user.getIncomeVerifiable())) {
            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.INCOME_PROOF,
                    "소득증빙이 필요한 상품입니다."
            ));
        } else {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.INCOME_PROOF,
                    "소득증빙 가능 여부 확인이 필요합니다."
            ));
        }
    }

    private void evaluateContractExclusion(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        Matcher matcher = CONTRACT_EXCLUSION.matcher(text);
        if (!matcher.find() || !"CONTRACT".equals(user.getEmploymentStatus())) {
            return;
        }

        int requiredMonths = Integer.parseInt(matcher.group(1)) * 12;
        Integer months = user.getEmploymentMonths();

        if (months == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.EMPLOYMENT_PERIOD,
                    "계약직 재직기간 확인이 필요합니다."
            ));
        } else if (months < requiredMonths) {
            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.EMPLOYMENT_PERIOD,
                    requiredMonths + "개월 미만 계약직은 이용할 수 없습니다."
            ));
        } else {
            results.add(ConditionEvaluation.satisfied(
                    ConditionType.EMPLOYMENT_PERIOD,
                    "계약직 재직기간 조건을 충족합니다."
            ));
        }
    }

    private void evaluateEmploymentType(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        String type = user.getEmploymentStatus();
        if (type == null || type.isBlank()) {
            return;
        }

        boolean businessExcluded = BUSINESS_EXCLUSION.matcher(text).find();
        if (businessExcluded && "BUSINESS".equals(type)) {
            results.add(ConditionEvaluation.notSatisfied(
                    ConditionType.EMPLOYMENT,
                    "개인사업자는 가입대상에서 제외됩니다."
            ));
            return;
        }

        if (businessExcluded) {
            text = BUSINESS_EXCLUSION.matcher(text).replaceAll(" ");
        }

        boolean studentTarget = containsAny(text, "학생", "대학생", "재학생", "캠퍼스");
        boolean nonStudentAlternative = containsAny(
                text,
                "근로자", "재직", "사업자", "자영업자", "소상공인",
                "취업준비생", "사회초년생", "미취업청년"
        );

        if (studentTarget && !nonStudentAlternative) {
            if (!"STUDENT".equals(type)) {
                results.add(ConditionEvaluation.notSatisfied(
                        ConditionType.EMPLOYMENT,
                        "학생 대상 상품입니다."
                ));
            } else if (containsAny(text, "대학생", "재학생")) {
                results.add(ConditionEvaluation.needsConfirmation(
                        ConditionType.EMPLOYMENT,
                        "학생 여부는 확인되었으나 대학 재학 등 세부 학적조건 확인이 필요합니다."
                ));
            } else {
                results.add(ConditionEvaluation.satisfied(
                        ConditionType.EMPLOYMENT,
                        "학생 대상 조건과 일치합니다."
                ));
            }
            return;
        }

        if (containsAny(text, "취업준비생", "사회초년생", "미취업청년")) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.EMPLOYMENT,
                    "취업준비생·사회초년생 세부 자격은 추가 확인이 필요합니다."
            ));
        }

        if (text.contains("정규직 또는 무기계약직") || text.contains("정규직·무기계약직")) {
            results.add("REGULAR".equals(type)
                    ? ConditionEvaluation.satisfied(ConditionType.EMPLOYMENT, "정규직·무기계약직 대상 조건을 충족합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.EMPLOYMENT, "정규직·무기계약직 대상 상품입니다."));
            return;
        }

        boolean alternativeTarget = text.contains("채무조정")
                || text.contains("금융취약")
                || text.contains("기초생활")
                || text.contains("차상위")
                || text.contains("전세피해")
                || text.contains("문화예술")
                || text.contains("장애인");

        boolean onlyBusinessTarget = (text.contains("소상공인") || text.contains("사업자") || text.contains("자영업자"))
                && !text.contains("근로자")
                && !text.contains("대학생")
                && !text.contains("학생")
                && !text.contains("취업준비생")
                && !text.contains("사회초년생")
                && !text.contains("미취업청년")
                && !text.contains("청년")
                && !alternativeTarget;

        if (onlyBusinessTarget) {
            results.add("BUSINESS".equals(type)
                    ? ConditionEvaluation.satisfied(ConditionType.EMPLOYMENT, "사업자·소상공인 대상 조건과 일치합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.EMPLOYMENT, "사업자·소상공인 대상 상품입니다."));
            return;
        }

        boolean explicitWorkerTarget = text.contains("청년 근로자")
                || text.contains("근로자 /")
                || text.contains("근로 중인 자")
                || text.contains("재직 근로자");

        boolean onlyWorkerTarget = text.contains("근로자")
                && !text.contains("사업자")
                && !text.contains("자영업자")
                && !text.contains("소상공인")
                && (explicitWorkerTarget || (!text.contains("대학생") && !text.contains("청년") && !alternativeTarget));

        if (onlyWorkerTarget) {
            boolean worker = switch (type) {
                case "REGULAR", "CONTRACT", "TEMPORARY", "DISPATCH", "DAILY" -> true;
                default -> false;
            };

            results.add(worker
                    ? ConditionEvaluation.satisfied(ConditionType.EMPLOYMENT, "근로자 대상 조건과 일치합니다.")
                    : ConditionEvaluation.notSatisfied(ConditionType.EMPLOYMENT, "근로자 대상 상품입니다."));
        }
    }

    private void evaluateEmploymentPeriod(
            String text,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        Integer requiredMonths = findRequiredMonths(text);
        if (requiredMonths == null) {
            return;
        }

        if (user.getEmploymentMonths() == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.EMPLOYMENT_PERIOD,
                    "재직 또는 사업기간 확인이 필요합니다."
            ));
            return;
        }

        results.add(user.getEmploymentMonths() >= requiredMonths
                ? ConditionEvaluation.satisfied(ConditionType.EMPLOYMENT_PERIOD, "재직·사업기간 조건을 충족합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.EMPLOYMENT_PERIOD, "재직·사업기간 조건을 충족하지 않습니다."));
    }

    private Integer findRequiredMonths(String text) {
        Matcher matcher = PERIOD_MONTHS_A.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        matcher = PERIOD_MONTHS_B.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        matcher = PERIOD_YEARS_A.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 12;
        }

        matcher = PERIOD_YEARS_B.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 12;
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
