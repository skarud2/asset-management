package com.via.shinvia.policy.recommendation.policyloan.service;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationResultDTO;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.model.ProductType;
import com.via.shinvia.policy.recommendation.common.model.RecommendationStatus;
import com.via.shinvia.policy.recommendation.common.parser.AgeConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.CreditConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.EmploymentConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.IncomeConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.NetAssetConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.RegionConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.WelfareConditionParser;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import com.via.shinvia.policy.recommendation.policyloan.dto.EligibilityJsonDTO;
import com.via.shinvia.policy.recommendation.policyloan.dto.PolicySupportProductDTO;
import com.via.shinvia.policy.recommendation.policyloan.mapper.PolicyLoanRecommendationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
// policy_support_program 기반 서민금융·정책대출 추천 기능
public class PolicyLoanRecommendationService {

    private final PolicyLoanRecommendationMapper mapper;
    private final AgeConditionParser ageParser;
    private final IncomeConditionParser incomeParser;
    private final NetAssetConditionParser netAssetParser;
    private final CreditConditionParser creditParser;
    private final RegionConditionParser regionParser;
    private final WelfareConditionParser welfareParser;
    private final EmploymentConditionParser employmentParser;
    private final ObjectMapper objectMapper;

    public List<RecommendationResultDTO> recommend(RecommendationUserDTO user) {
        List<RecommendationResultDTO> results = new ArrayList<>();

        for (PolicySupportProductDTO product : mapper.findActiveProducts()) {
            RecommendationResultDTO result = evaluateProduct(user, product);
            results.add(result);
        }

        return results.stream()
                .sorted(Comparator.comparingInt(RecommendationResultDTO::getMatchScore).reversed())
                .toList();
    }

    private RecommendationResultDTO evaluateProduct(
            RecommendationUserDTO user,
            PolicySupportProductDTO product
    ) {
        EligibilityJsonDTO json = parseEligibilityJson(product.getEligibilityJson());
        List<ConditionEvaluation> conditions = new ArrayList<>();

        if (product.getEligibilityJson() != null
                && !product.getEligibilityJson().isBlank()
                && json == null) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.INSTITUTION_REVIEW,
                    "상품의 구조화된 자격조건을 해석하지 못해 기관 확인이 필요합니다."
            ));
        }

        String eligibility = safe(product.getEligibilityDescription());
        String target = safe(product.getTargetDescription());
        String ageText = join(json == null ? null : json.getAge(), eligibility);
        String incomeText = join(json == null ? null : json.getIncome(), eligibility);
        String creditText = join(
                json == null ? null : json.getCreditScore(),
                json == null ? null : json.getIncome(),
                eligibility,
                json == null ? null : json.getEtcReference()
        );
        String welfareText = join(
                target,
                json == null ? null : json.getIncome(),
                eligibility
        );
        String employmentText = join(
                product.getProductName(),
                target,
                eligibility,
                json == null ? null : json.getEtcReference()
        );
        String regionDetail = join(
                json == null ? null : json.getResidenceArea(),
                target,
                eligibility
        );

        addAllUnique(conditions, regionParser.evaluate(product.getSupportArea(), regionDetail, user));
        addAllUnique(conditions, ageParser.evaluate(ageText, user));
        addAllUnique(conditions, incomeParser.evaluate(incomeText, user));
        addAllUnique(conditions, netAssetParser.evaluate(
                join(
                        json == null ? null : json.getHouseholdCondition(),
                        eligibility,
                        target
                ),
                user
        ));
        addAllUnique(conditions, creditParser.evaluate(creditText, user));
        addAllUnique(conditions, welfareParser.evaluate(welfareText, user));
        addAllUnique(conditions, employmentParser.evaluate(employmentText, user));

        evaluateHouseholdCondition(user, json, join(target, eligibility), conditions);
        evaluatePolicyFinanceUsage(user, join(target, eligibility), conditions);
        evaluateFinancialEducation(user, json, eligibility, conditions);
        evaluateManualReview(product, json, conditions);
        evaluateHousingInformationCoverage(product, conditions);
        ensureMinimumConditionCoverage(conditions);

        RecommendationStatus status = determineStatus(conditions);
        int score = calculateScore(user, product, conditions, status);

        return RecommendationResultDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productType(ProductType.POLICY_LOAN)
                .institutionName(firstNotBlank(
                        product.getOfferingInstitutionName(),
                        product.getHandlingInstitution()
                ))
                .status(status)
                .matchScore(score)
                .primaryBenefit(formatAmount(product.getMaxSupportAmount()))
                .secondaryBenefit(formatRate(product))
                .supportArea(product.getSupportArea())
                .targetDescription(product.getTargetDescription())
                .eligibilityDescription(product.getEligibilityDescription())
                .applicationMethod(product.getApplicationMethod())
                .relatedUrl(product.getApplicationUrl())
                .conditions(conditions)
                .build();
    }

    private void evaluateHouseholdCondition(
            RecommendationUserDTO user,
            EligibilityJsonDTO json,
            String eligibility,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(join(
                json == null ? null : json.getHouseholdCondition(),
                eligibility
        ));

        if (text.isBlank()) {
            return;
        }

        if (text.contains("무주택")) {
            addUnique(conditions, evaluateBooleanCondition(
                    user.getHomelessHousehold(),
                    ConditionType.HOUSEHOLD,
                    "무주택 가구 조건을 충족합니다.",
                    "주택을 보유한 가구는 이용할 수 없는 상품입니다.",
                    "무주택 가구 여부 확인이 필요합니다."
            ));
        }

        if (text.contains("세대주")) {
            boolean allowsProspectiveHead = text.contains("예비세대주");
            Boolean qualifiesAsHead = Boolean.TRUE.equals(user.getHouseholdHead())
                    || (allowsProspectiveHead && Boolean.TRUE.equals(user.getProspectiveHouseholdHead()));
            addUnique(conditions, evaluateBooleanCondition(
                    qualifiesAsHead ? Boolean.TRUE
                            : user.getHouseholdHead() == null && user.getProspectiveHouseholdHead() == null
                            ? null
                            : Boolean.FALSE,
                    ConditionType.HOUSEHOLD,
                    allowsProspectiveHead ? "세대주·예비세대주 조건을 충족합니다." : "세대주 조건을 충족합니다.",
                    allowsProspectiveHead ? "세대주 또는 예비세대주를 대상으로 하는 상품입니다." : "세대주를 대상으로 하는 상품입니다.",
                    allowsProspectiveHead ? "세대주·예비세대주 여부 확인이 필요합니다." : "세대주 여부 확인이 필요합니다."
            ));
        }

        if (text.contains("부양의무자") || text.contains("가구원")) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.HOUSEHOLD,
                    "부양의무자·가구원 세부조건은 추가 확인이 필요합니다."
            ));
        }

        if (text.contains("생애최초") || text.contains("생애 최초")) {
            boolean hasAlternativeTarget = containsAny(
                    text,
                    "또는", "신혼", "다자녀", "2자녀", "신생아", "출산가구"
            );

            if (Boolean.TRUE.equals(user.getFirstTimeHomeBuyer())) {
                addUnique(conditions, ConditionEvaluation.satisfied(
                        ConditionType.HOUSEHOLD,
                        "생애최초 주택구입자 조건에 해당합니다."
                ));
            } else if (Boolean.FALSE.equals(user.getFirstTimeHomeBuyer()) && !hasAlternativeTarget) {
                addUnique(conditions, ConditionEvaluation.notSatisfied(
                        ConditionType.HOUSEHOLD,
                        "생애최초 주택구입자를 대상으로 하는 상품입니다."
                ));
            } else {
                addUnique(conditions, ConditionEvaluation.needsConfirmation(
                        ConditionType.HOUSEHOLD,
                        hasAlternativeTarget
                                ? "생애최초 외 신혼·다자녀 등 대체 대상자 조건 확인이 필요합니다."
                                : "생애최초 주택구입 여부 확인이 필요합니다."
                ));
            }
        }
    }

    private void evaluatePolicyFinanceUsage(
            RecommendationUserDTO user,
            String rawText,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(rawText);

        // 설문의 policyFinanceUsage는 '정책서민금융 이용 경험'만 의미한다.
        // 채무조정 이력은 별도 값이 없으므로 정책금융 이용여부로 대체 판정하지 않는다.
        if (text.contains("채무조정") && text.contains("성실상환")) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.POLICY_FINANCE_USAGE,
                    "채무조정 성실상환 이력은 현재 설문만으로 확인할 수 없습니다."
            ));
        }

        boolean requiresPolicyFinanceHistory = text.contains("정책서민금융")
                || (text.contains("미소금융") && text.contains("성실상환"));

        if (!requiresPolicyFinanceHistory) {
            return;
        }

        String usage = user.getPolicyFinanceUsage();
        if ("NONE".equals(usage)) {
            addUnique(conditions, ConditionEvaluation.notSatisfied(
                    ConditionType.POLICY_FINANCE_USAGE,
                    "정책서민금융 이용이력이 필요한 상품입니다."
            ));
        } else if ("CURRENT".equals(usage) || "PAST".equals(usage)) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.POLICY_FINANCE_USAGE,
                    "정책서민금융 이용이력은 있으나 성실상환 기간·완제 시점 등 세부 확인이 필요합니다."
            ));
        } else {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.POLICY_FINANCE_USAGE,
                    "정책서민금융 이용이력 확인이 필요합니다."
            ));
        }
    }

    private void evaluateFinancialEducation(
            RecommendationUserDTO user,
            EligibilityJsonDTO json,
            String eligibility,
            List<ConditionEvaluation> conditions
    ) {
        String text = join(
                eligibility,
                json == null ? null : json.getFinancialEducationProductEtc()
        );
        String normalized = RegionNormalizer.normalizeText(text);

        boolean required = normalized.contains("금융교육")
                && (normalized.contains("이수") || normalized.contains("필수"));

        if (!required) {
            return;
        }

        if ("YES".equals(user.getFinancialEducationStatus())) {
            addUnique(conditions, ConditionEvaluation.satisfied(
                    ConditionType.FINANCIAL_EDUCATION,
                    "금융교육 이수 조건을 충족합니다."
            ));
        } else if ("NO".equals(user.getFinancialEducationStatus())) {
            // 교육을 아직 이수하지 않았다고 해서 상품 자체가 영구적으로 불가능한 것은 아니므로 확인필요로 둔다.
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.FINANCIAL_EDUCATION,
                    "상품에서 요구하는 금융교육 이수 방법과 시점을 확인해야 합니다."
            ));
        } else {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.FINANCIAL_EDUCATION,
                    "금융교육 이수 여부 확인이 필요합니다."
            ));
        }
    }

    private void evaluateManualReview(
            PolicySupportProductDTO product,
            EligibilityJsonDTO json,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(join(
                product.getProductName(),
                product.getEligibilityDescription(),
                json == null ? null : json.getCreditScore(),
                json == null ? null : json.getEtcReference()
        ));

        if (containsAny(text,
                "적격여부", "적격 여부", "심사를 통해", "심사 시", "최종 심사",
                "신용평가시스템", "신용평가모형", "평가등급", "선별된",
                "추천을 받은", "추천받은", "추천서", "위원회", "피해사실확인서",
                "사업성이 양호", "재기 가능", "도덕성 검토", "성과지표",
                "컨설팅", "사업계획서", "창업계획서", "확인서를 발급",
                "보증잔액", "구상채권", "소송", "피해기업",
                "특허", "실용신안", "인증기업", "인증받은", "수출실적",
                "업종 영위", "사업장 소재", "사업장이", "재창업", "폐업 후",
                "민간사업수행기관", "임차보증금", "주택가격", "주택가액",
                "비정상거처확인서", "주거상향 유형 확인서")) {

            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.INSTITUTION_REVIEW,
                    "기관의 추천·심사·확인서 등 별도 자격확인이 필요한 상품입니다."
            ));
        }
    }

    private void evaluateHousingInformationCoverage(
            PolicySupportProductDTO product,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(join(
                product.getProductName(),
                product.getUsageDescription(),
                product.getTargetDescription(),
                product.getEligibilityDescription()
        ));

        if (containsAny(text, "월세", "전세", "주택", "주거")
                && conditions.stream().noneMatch(condition ->
                condition.getType() == ConditionType.HOUSEHOLD)) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.HOUSEHOLD,
                    "주거·임차 관련 상품으로 주택 및 임대차 세부조건 확인이 필요합니다."
            ));
        }
    }

    private void ensureMinimumConditionCoverage(List<ConditionEvaluation> conditions) {
        boolean failed = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NOT_SATISFIED);
        boolean needsCheck = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NEEDS_CONFIRMATION);
        long satisfiedTypes = conditions.stream()
                .filter(condition -> condition.getStatus() == ConditionStatus.SATISFIED)
                .map(ConditionEvaluation::getType)
                .distinct()
                .count();

        if (!failed && !needsCheck && satisfiedTypes == 1) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.INSTITUTION_REVIEW,
                    "현재 정보로 확인된 핵심 가입조건이 충분하지 않아 상품 세부조건 확인이 필요합니다."
            ));
        }
    }

    private RecommendationStatus determineStatus(List<ConditionEvaluation> conditions) {
        boolean failed = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NOT_SATISFIED);

        if (failed) {
            return RecommendationStatus.INELIGIBLE;
        }

        boolean satisfied = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.SATISFIED);

        if (!satisfied) {
            return RecommendationStatus.UNVERIFIABLE;
        }

        boolean needsCheck = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NEEDS_CONFIRMATION);

        if (needsCheck) {
            return RecommendationStatus.NEEDS_CONFIRMATION;
        }

        return RecommendationStatus.ELIGIBLE;
    }

    private int calculateScore(
            RecommendationUserDTO user,
            PolicySupportProductDTO product,
            List<ConditionEvaluation> conditions,
            RecommendationStatus status
    ) {
        int score = switch (status) {
            case ELIGIBLE -> 100;
            case NEEDS_CONFIRMATION -> 70;
            case UNVERIFIABLE -> 20;
            case INELIGIBLE -> 0;
        };

        long satisfied = conditions.stream()
                .filter(condition -> condition.getStatus() == ConditionStatus.SATISFIED)
                .count();
        long checks = conditions.stream()
                .filter(condition -> condition.getStatus() == ConditionStatus.NEEDS_CONFIRMATION)
                .count();

        score += (int) satisfied * 5;
        score -= (int) checks * 2;
        score += purposeBonus(user.getDesiredSupportPurpose(), product.getUsageDescription());
        score += amountBonus(user.getDesiredAmount(), product.getMaxSupportAmount());

        if ("ELIGIBILITY".equals(user.getPriorityPreference()) && status == RecommendationStatus.ELIGIBLE) {
            score += 15;
        }

        if ("RATE".equals(user.getPriorityPreference()) && product.getMinInterestRate() != null) {
            double rate = product.getMinInterestRate().doubleValue();
            score += Math.max(0, 12 - (int) Math.ceil(rate));
        }

        if ("LIMIT".equals(user.getPriorityPreference())
                && user.getDesiredAmount() != null
                && product.getMaxSupportAmount() != null
                && product.getMaxSupportAmount().compareTo(user.getDesiredAmount()) >= 0) {
            score += 12;
        }

        return score;
    }

    private int purposeBonus(String desiredPurpose, String usageDescription) {
        if (desiredPurpose == null || desiredPurpose.isBlank() || "UNKNOWN".equals(desiredPurpose)) {
            return 0;
        }

        if ("ASSET".equals(desiredPurpose)) {
            return -20;
        }

        String usage = RegionNormalizer.normalizeText(usageDescription);

        boolean match = switch (desiredPurpose) {
            case "LIVING" -> usage.contains("생계") || usage.contains("생활");
            case "REFINANCE" -> usage.contains("대환") || usage.contains("저금리전환") || usage.contains("저금리 전환");
            case "HOUSING" -> usage.contains("주거") || usage.contains("전세") || usage.contains("주택");
            case "BUSINESS" -> usage.contains("운영") || usage.contains("운전") || usage.contains("시설") || usage.contains("창업");
            case "EDUCATION" -> usage.contains("학자금") || usage.contains("교육");
            default -> false;
        };

        return match ? 25 : 0;
    }

    private int amountBonus(BigDecimal desiredAmount, BigDecimal maxSupportAmount) {
        if (desiredAmount == null || maxSupportAmount == null) {
            return 0;
        }

        return maxSupportAmount.compareTo(desiredAmount) >= 0 ? 10 : -5;
    }

    private EligibilityJsonDTO parseEligibilityJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, EligibilityJsonDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "지원한도 기관 확인";
        }

        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return "최대 " + format.format(amount) + "원";
    }

    private String formatRate(PolicySupportProductDTO product) {
        BigDecimal min = product.getMinInterestRate();
        BigDecimal max = product.getMaxInterestRate();

        if (min == null && max == null) {
            return firstNotBlank(product.getInterestRateDescription(), "금리 기관 확인");
        }

        if (min != null && max != null && min.compareTo(max) == 0) {
            return "연 " + min.stripTrailingZeros().toPlainString() + "%";
        }

        if (min == null) {
            return "연 " + max.stripTrailingZeros().toPlainString() + "% 이하";
        }

        if (max == null) {
            return "연 " + min.stripTrailingZeros().toPlainString() + "% 이상";
        }

        return "연 " + min.stripTrailingZeros().toPlainString()
                + "~" + max.stripTrailingZeros().toPlainString() + "%";
    }

    private void addAllUnique(
            List<ConditionEvaluation> target,
            List<ConditionEvaluation> source
    ) {
        for (ConditionEvaluation evaluation : source) {
            addUnique(target, evaluation);
        }
    }

    private ConditionEvaluation evaluateBooleanCondition(
            Boolean actual,
            ConditionType type,
            String satisfiedDescription,
            String failedDescription,
            String unknownDescription
    ) {
        if (Boolean.TRUE.equals(actual)) {
            return ConditionEvaluation.satisfied(type, satisfiedDescription);
        }
        if (Boolean.FALSE.equals(actual)) {
            return ConditionEvaluation.notSatisfied(type, failedDescription);
        }
        return ConditionEvaluation.needsConfirmation(type, unknownDescription);
    }

    private void addUnique(
            List<ConditionEvaluation> target,
            ConditionEvaluation evaluation
    ) {
        boolean duplicated = target.stream().anyMatch(existing ->
                existing.getType() == evaluation.getType()
                        && existing.getStatus() == evaluation.getStatus()
                        && safe(existing.getDescription()).equals(safe(evaluation.getDescription()))
        );

        if (!duplicated) {
            target.add(evaluation);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNoCondition(String value) {
        String text = RegionNormalizer.normalizeText(value);
        return text.isBlank() || "없음".equals(text) || "0".equals(text) || "null".equals(text);
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String firstNotBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
