package com.via.shinvia.policy.recommendation.asset.service;

import com.via.shinvia.policy.recommendation.asset.dto.AssetFormationProductDTO;
import com.via.shinvia.policy.recommendation.asset.mapper.AssetRecommendationMapper;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationResultDTO;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionStatus;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.model.ProductType;
import com.via.shinvia.policy.recommendation.common.model.RecommendationStatus;
import com.via.shinvia.policy.recommendation.common.parser.AgeConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.EmploymentConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.IncomeConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.RegionConditionParser;
import com.via.shinvia.policy.recommendation.common.parser.WelfareConditionParser;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
// asset_formation_product 기반 자산형성 상품 추천 기능
public class AssetRecommendationService {

    private final AssetRecommendationMapper mapper;
    private final AgeConditionParser ageParser;
    private final IncomeConditionParser incomeParser;
    private final RegionConditionParser regionParser;
    private final WelfareConditionParser welfareParser;
    private final EmploymentConditionParser employmentParser;

    public List<RecommendationResultDTO> recommend(RecommendationUserDTO user) {
        List<RecommendationResultDTO> results = new ArrayList<>();

        for (AssetFormationProductDTO product : mapper.findActiveProducts()) {
            RecommendationResultDTO result = evaluateProduct(user, product);
            results.add(result);
        }

        return results.stream()
                .sorted(Comparator.comparingInt(RecommendationResultDTO::getMatchScore).reversed())
                .toList();
    }

    private RecommendationResultDTO evaluateProduct(
            RecommendationUserDTO user,
            AssetFormationProductDTO product
    ) {
        List<ConditionEvaluation> conditions = new ArrayList<>();
        String target = safe(product.getSubscriptionTarget());

        addAllUnique(conditions, regionParser.evaluate(
                product.getSupportRegion(),
                target,
                user
        ));

        addAllUnique(conditions, ageParser.evaluate(
                join(product.getAgeCondition(), target),
                user
        ));

        addAllUnique(conditions, incomeParser.evaluate(
                join(product.getIncomeCondition(), target),
                user
        ));

        addAllUnique(conditions, welfareParser.evaluate(target, user));
        addAllUnique(conditions, employmentParser.evaluate(
                join(product.getProductName(), target),
                user
        ));

        evaluateHouseholdAndFamily(target, conditions);
        evaluateEducation(user, join(target, product.getMaturityBenefit()), conditions);
        evaluateSavingCapacity(user, product.getGovernmentSupport(), conditions);
        evaluateManualReview(product, conditions);
        ensureMinimumConditionCoverage(conditions);

        // 가입제한이 사실상 없는 일반 적금은 빈 조건 때문에 확인필요가 되지 않도록 처리
        if (conditions.isEmpty() && isOpenToGeneralIndividual(target)) {
            conditions.add(ConditionEvaluation.satisfied(
                    ConditionType.OTHER,
                    "별도의 주요 가입자격 제한이 없는 상품입니다."
            ));
        }

        RecommendationStatus status = determineStatus(conditions);
        int score = calculateScore(user, product, conditions, status);

        return RecommendationResultDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productType(ProductType.ASSET)
                .institutionName(product.getInstitutionName())
                .status(status)
                .matchScore(score)
                .primaryBenefit(firstNotBlank(product.getGovernmentSupport(), "지원혜택 상세 확인"))
                .secondaryBenefit(firstNotBlank(product.getSubscriptionPeriod(), product.getSavingMethod()))
                .supportArea(product.getSupportRegion())
                .targetDescription(product.getSubscriptionTarget())
                .eligibilityDescription(product.getIncomeCondition())
                .applicationMethod(product.getApplicationMethod())
                .relatedUrl(product.getRelatedUrl())
                .conditions(conditions)
                .build();
    }

    private void evaluateHouseholdAndFamily(
            String rawText,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(rawText);

        if (text.contains("부양의무자")
                || text.contains("자녀를 키우")
                || text.contains("아동을 양육")
                || text.contains("가구당")
                || text.contains("가구원 수")) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.HOUSEHOLD,
                    "가구원·부양의무자·자녀 세부조건은 추가 확인이 필요합니다."
            ));
        }
    }

    private void evaluateEducation(
            RecommendationUserDTO user,
            String rawText,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(rawText);
        boolean required = (text.contains("교육") && text.contains("필수"))
                || text.contains("교육 이수")
                || text.contains("교육이수");

        if (!required) {
            return;
        }

        if ("YES".equals(user.getFinancialEducationStatus())) {
            addUnique(conditions, ConditionEvaluation.satisfied(
                    ConditionType.FINANCIAL_EDUCATION,
                    "금융교육 이수 조건을 충족합니다."
            ));
        } else {
            // 상품별 교육과 사용자의 과거 금융교육이 동일 교육인지 알 수 없으므로 탈락시키지 않는다.
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.FINANCIAL_EDUCATION,
                    "상품에서 요구하는 교육 이수 여부는 별도 확인이 필요합니다."
            ));
        }
    }

    private void evaluateSavingCapacity(
            RecommendationUserDTO user,
            String governmentSupport,
            List<ConditionEvaluation> conditions
    ) {
        if (user.getMonthlySavingCapacity() == null || governmentSupport == null) {
            return;
        }

        // government_support 앞부분은 대체로 "월 10만원 / 월 10만원 매칭"처럼
        // 이용자 월 납입액을 먼저 제공한다. 최소 월 납입액만 보수적으로 추출한다.
        String firstPart = RegionNormalizer.normalizeText(governmentSupport).split("/", 2)[0];
        Matcher matcher = Pattern.compile("(?:월\\s*)?(\\d[\\d,]*(?:\\.\\d+)?)\\s*(만원|천원|원)").matcher(firstPart);

        BigDecimal minimum = null;
        while (matcher.find()) {
            BigDecimal value = toWon(matcher.group(1), matcher.group(2));
            if (minimum == null || value.compareTo(minimum) < 0) {
                minimum = value;
            }
        }

        if (minimum == null) {
            return;
        }

        if (user.getMonthlySavingCapacity().compareTo(minimum) >= 0) {
            addUnique(conditions, ConditionEvaluation.satisfied(
                    ConditionType.AMOUNT,
                    "월 저축 가능금액이 상품의 최소 납입 수준 이상입니다."
            ));
        } else {
            // 월 저축 가능금액은 사용자의 예상값이므로 확정 탈락 대신 확인필요로 둔다.
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.AMOUNT,
                    "현재 입력한 월 저축 가능금액으로는 최소 납입액 충족 여부를 확인해야 합니다."
            ));
        }
    }

    private BigDecimal toWon(String numberText, String unit) {
        BigDecimal number = new BigDecimal(numberText.replace(",", ""));
        BigDecimal multiplier = switch (unit) {
            case "만원" -> BigDecimal.valueOf(10_000L);
            case "천원" -> BigDecimal.valueOf(1_000L);
            default -> BigDecimal.ONE;
        };
        return number.multiply(multiplier);
    }

    private void evaluateManualReview(
            AssetFormationProductDTO product,
            List<ConditionEvaluation> conditions
    ) {
        String text = RegionNormalizer.normalizeText(join(
                product.getSubscriptionTarget(),
                product.getIncomeCondition(),
                product.getMaturityBenefit()
        ));

        if (containsAny(text,
                "선정된", "선정기준", "최종 가입자 선정", "추첨", "가입승인",
                "추천서", "증빙서류", "증빙 서류", "확인서", "고용보험",
                "건강보험료", "중복가입 불가", "중복 가입 불가", "유사 사업 참여",
                "금융소득종합과세", "보호 결정", "쉼터", "자립지원관",
                "재학증명서", "학생증", "지원제외 대상", "지원 제외 대상")) {

            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.INSTITUTION_REVIEW,
                    "선정·증빙·중복가입 등 별도 확인조건이 있는 상품입니다."
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

    private void ensureMinimumConditionCoverage(List<ConditionEvaluation> conditions) {
        boolean failed = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NOT_SATISFIED);
        boolean needsCheck = conditions.stream()
                .anyMatch(condition -> condition.getStatus() == ConditionStatus.NEEDS_CONFIRMATION);
        boolean openProduct = conditions.stream()
                .anyMatch(condition -> condition.getType() == ConditionType.OTHER
                        && condition.getStatus() == ConditionStatus.SATISFIED);
        long satisfiedTypes = conditions.stream()
                .filter(condition -> condition.getStatus() == ConditionStatus.SATISFIED)
                .map(ConditionEvaluation::getType)
                .distinct()
                .count();

        if (!failed && !needsCheck && !openProduct && satisfiedTypes == 1) {
            addUnique(conditions, ConditionEvaluation.needsConfirmation(
                    ConditionType.INSTITUTION_REVIEW,
                    "현재 정보로 확인된 핵심 가입조건이 충분하지 않아 상품 세부조건 확인이 필요합니다."
            ));
        }
    }

    private int calculateScore(
            RecommendationUserDTO user,
            AssetFormationProductDTO product,
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

        if ("ASSET".equals(user.getDesiredSupportPurpose())) {
            score += 35;
        } else if (user.getDesiredSupportPurpose() != null
                && !user.getDesiredSupportPurpose().isBlank()
                && !"UNKNOWN".equals(user.getDesiredSupportPurpose())) {
            // 대출·주거·생계 등 다른 목적을 선택했다면 자산형성상품은 전체 탭에서 후순위로 둔다.
            score -= 25;
        }

        if ("BENEFIT".equals(user.getPriorityPreference())
                && product.getGovernmentSupport() != null
                && !product.getGovernmentSupport().isBlank()) {
            score += 15;
        }

        if ("PERIOD".equals(user.getPriorityPreference())
                && product.getSubscriptionPeriod() != null
                && !product.getSubscriptionPeriod().isBlank()) {
            score += 5;
        }

        if ("ELIGIBILITY".equals(user.getPriorityPreference())
                && status == RecommendationStatus.ELIGIBLE) {
            score += 15;
        }

        return score;
    }

    private boolean isOpenToGeneralIndividual(String rawText) {
        String text = RegionNormalizer.normalizeText(rawText);
        return text.contains("가입대상에 특별한 제한은 없음")
                || text.contains("실명의 개인")
                || text.contains("개인 1인 1계좌");
    }

    private void addAllUnique(
            List<ConditionEvaluation> target,
            List<ConditionEvaluation> source
    ) {
        for (ConditionEvaluation evaluation : source) {
            addUnique(target, evaluation);
        }
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

    private String firstNotBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
