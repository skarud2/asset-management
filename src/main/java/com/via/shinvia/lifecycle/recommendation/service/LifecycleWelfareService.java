package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LifecycleWelfareService {

    private static final int RECOMMENDATION_LIMIT = 5;
    private static final String SOURCE_KINFA = "KINFA";
    private static final String SOURCE_BOKJIRO_LOCAL = "BOKJIRO_LOCAL";
    private static final String SOURCE_BOKJIRO_NATIONAL = "BOKJIRO_NATIONAL";

    private final WelfareSupportProductRepository welfareSupportProductRepository;
    private final LifecycleEligibilityService lifecycleEligibilityService;

    public List<LifecycleSupportDto> getSupports(
            LifecycleEventType eventType,
            String regionSido,
            String regionSigungu,
            Long userId
    ) {
        List<KeywordRule> keywordRules = resolveKeywordRules(eventType);
        if (keywordRules.isEmpty()) {
            return List.of();
        }

        List<WelfareSupportProduct> candidates = welfareSupportProductRepository.findLifecycleCandidates(
                        keywordRules.stream().map(KeywordRule::keyword).toList(),
                        regionSido,
                        regionSigungu
                );

        var userContext = lifecycleEligibilityService != null
                ? lifecycleEligibilityService.buildUserContext(userId, null, regionSido, regionSigungu, null)
                : null;

        return deduplicateCrossSource(candidates).stream()
                .filter(product -> isRegionEligible(
                        product,
                        regionSido,
                        regionSigungu
                ))
                .filter(product -> isRelevantForEvent(product, eventType))
                .sorted(recommendationOrder(keywordRules, regionSido, regionSigungu))
                .limit(RECOMMENDATION_LIMIT)
                .map(product -> toLifecycleSupport(product, userContext))
                .toList();
    }

    public List<LifecycleSupportDto> getSupports(
            LifecycleEventType eventType,
            String regionSido,
            String regionSigungu
    ) {
        return getSupports(eventType, regionSido, regionSigungu, null);
    }

    private List<WelfareSupportProduct> deduplicateCrossSource(
            List<WelfareSupportProduct> candidates
    ) {
        Map<String, List<WelfareSupportProduct>> byName = new LinkedHashMap<>();
        for (WelfareSupportProduct candidate : candidates) {
            byName.computeIfAbsent(normalize(candidate.getProductName()), ignored -> new ArrayList<>())
                    .add(candidate);
        }

        List<WelfareSupportProduct> result = new ArrayList<>();
        for (List<WelfareSupportProduct> sameName : byName.values()) {
            List<WelfareSupportProduct> bokjiro = sameName.stream()
                    .filter(this::isBokjiro)
                    .toList();

            for (WelfareSupportProduct candidate : sameName) {
                if (!isKinfa(candidate)
                        || bokjiro.stream().noneMatch(item -> isCrossSourceDuplicate(candidate, item))) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private boolean isCrossSourceDuplicate(
            WelfareSupportProduct kinfa,
            WelfareSupportProduct bokjiro
    ) {
        if (!normalize(kinfa.getProductName()).equals(normalize(bokjiro.getProductName()))) {
            return false;
        }

        if (SOURCE_BOKJIRO_NATIONAL.equals(bokjiro.getSourceType())) {
            return true;
        }

        if (!SOURCE_BOKJIRO_LOCAL.equals(bokjiro.getSourceType())) {
            return false;
        }

        return institutionsMatch(kinfa, bokjiro)
                || regionIsMentionedByKinfa(kinfa, bokjiro);
    }

    private boolean institutionsMatch(
            WelfareSupportProduct first,
            WelfareSupportProduct second
    ) {
        List<String> firstInstitutions = institutionNames(first);
        List<String> secondInstitutions = institutionNames(second);

        return firstInstitutions.stream().anyMatch(firstName ->
                secondInstitutions.stream().anyMatch(secondName ->
                        firstName.equals(secondName)
                                || (firstName.length() >= 4 && secondName.contains(firstName))
                                || (secondName.length() >= 4 && firstName.contains(secondName))
                )
        );
    }

    private List<String> institutionNames(WelfareSupportProduct product) {
        return List.of(
                        normalize(product.getInstitutionName()),
                        normalize(product.getResponsibleInstitution())
                ).stream()
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private boolean regionIsMentionedByKinfa(
            WelfareSupportProduct kinfa,
            WelfareSupportProduct local
    ) {
        String kinfaText = normalize(String.join(" ",
                value(kinfa.getProductName()),
                value(kinfa.getInstitutionName()),
                value(kinfa.getResponsibleInstitution()),
                value(kinfa.getSupportTarget()),
                value(kinfa.getSupportContent())
        ));
        String sido = normalize(local.getRegionSido());
        String sigungu = normalize(local.getRegionSigungu());

        return (!sigungu.isEmpty() && kinfaText.contains(sigungu))
                || (!sido.isEmpty() && kinfaText.contains(sido));
    }

    private Comparator<WelfareSupportProduct> recommendationOrder(
            List<KeywordRule> keywordRules,
            String regionSido,
            String regionSigungu
    ) {
        return Comparator
                .comparingInt((WelfareSupportProduct product) ->
                        recommendationScore(product, keywordRules, regionSido, regionSigungu))
                .reversed()
                .thenComparing(
                        Comparator.comparingInt((WelfareSupportProduct product) ->
                                regionScore(product, regionSido, regionSigungu)).reversed()
                )
                .thenComparingInt(this::sourcePriority)
                .thenComparing(
                        WelfareSupportProduct::getSourceUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        WelfareSupportProduct::getWelfareSupportProductId,
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private int recommendationScore(
            WelfareSupportProduct product,
            List<KeywordRule> keywordRules,
            String regionSido,
            String regionSigungu
    ) {
        int score = regionScore(product, regionSido, regionSigungu);
        score += keywordRules.stream()
                .mapToInt(rule -> keywordScore(product, rule))
                .max()
                .orElse(0);
        if (hasText(product.getApplicationMethod()) && hasText(product.getRelatedUrl())) {
            score += 5;
        }
        return score;
    }

    private int keywordScore(WelfareSupportProduct product, KeywordRule rule) {
        if (contains(product.getProductName(), rule.keyword())) {
            return rule.titleScore();
        }
        if (contains(searchableDetails(product), rule.keyword())) {
            return rule.detailScore();
        }
        return 0;
    }

    private int regionScore(
            WelfareSupportProduct product,
            String regionSido,
            String regionSigungu
    ) {
        if (SOURCE_BOKJIRO_LOCAL.equals(product.getSourceType())) {
            if (sameText(product.getRegionSido(), regionSido)
                    && sameText(product.getRegionSigungu(), regionSigungu)) {
                return 40;
            }
            if (sameText(product.getRegionSido(), regionSido)) {
                return 25;
            }
        }
        return 20;
    }

    private int sourcePriority(WelfareSupportProduct product) {
        if (isBokjiro(product)) {
            return 0;
        }
        if (isKinfa(product)) {
            return 1;
        }
        return 2;
    }

    private String searchableDetails(WelfareSupportProduct product) {
        return String.join(" ",
                value(product.getInstitutionName()),
                value(product.getSupportTarget()),
                value(product.getAgeCondition()),
                value(product.getWelfareType()),
                value(product.getSupportContent()),
                value(product.getApplicationMethod()),
                value(product.getResponsibleInstitution()),
                value(product.getSupportCycle()),
                value(product.getSupportMethod())
        );
    }

    /**
     * 대상자 키워드와 실제 지원 목적을 분리한다.
     * 예를 들어 "신혼부부 전세자금 대출이자 지원"은 신혼부부 대상이지만
     * 결혼 비용 지원이 아니라 주거 계약을 위한 지원이므로 주거 이벤트에서 추천한다.
     */
    private boolean isRelevantForEvent(
            WelfareSupportProduct product,
            LifecycleEventType eventType
    ) {
        String productName = normalize(product.getProductName());
        if (productName.isEmpty()) {
            return false;
        }

        List<String> purposeKeywords = switch (eventType) {
            case MARRIAGE -> List.of(
                    "결혼장려", "결혼축하", "결혼지원금",
                    "결혼비용", "결혼자금", "혼례비"
            );
            case MONTHLY_RENT -> List.of(
                    "월세지원", "월세대출", "임차료지원", "월임차료"
            );
            case JEONSE -> List.of(
                    "전세자금", "전세대출", "전세보증금",
                    "임차보증금", "전월세보증금"
            );
            case HOME_PURCHASE -> List.of(
                    "주택구입", "주택구매", "내집마련", "구입자금",
                    "주택자금", "디딤돌", "모기지", "보금자리론"
            );
            default -> List.of();
        };

        if (purposeKeywords.isEmpty()) {
            return true;
        }

        return purposeKeywords.stream()
                .map(this::normalize)
                .anyMatch(productName::contains);
    }

    private boolean isRegionEligible(
            WelfareSupportProduct product,
            String regionSido,
            String regionSigungu
    ) {
        if (!SOURCE_BOKJIRO_LOCAL.equals(product.getSourceType())) {
            return true;
        }

        // 거주지를 모르면 다른 지역의 지자체 지원을 임의로 추천하지 않는다.
        if (!hasText(regionSido)) {
            return false;
        }

        if (!sameText(product.getRegionSido(), regionSido)) {
            return false;
        }

        return !hasText(regionSigungu)
                || !hasText(product.getRegionSigungu())
                || sameText(product.getRegionSigungu(), regionSigungu);
    }

    private boolean isBokjiro(WelfareSupportProduct product) {
        return SOURCE_BOKJIRO_LOCAL.equals(product.getSourceType())
                || SOURCE_BOKJIRO_NATIONAL.equals(product.getSourceType());
    }

    private boolean isKinfa(WelfareSupportProduct product) {
        return SOURCE_KINFA.equals(product.getSourceType());
    }

    private boolean sameText(String first, String second) {
        return hasText(first) && hasText(second) && normalize(first).equals(normalize(second));
    }

    private boolean contains(String text, String keyword) {
        return hasText(text) && hasText(keyword)
                && normalize(text).contains(normalize(keyword));
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private List<KeywordRule> resolveKeywordRules(LifecycleEventType eventType) {
        if (eventType == null) {
            return List.of();
        }

        return switch (eventType) {
            case MARRIAGE -> rules(
                    "결혼", "신혼", "혼인", "예비부부"
            );
            case CHILDBIRTH -> rules(
                    "출산", "출생", "산모", "임산부", "신생아", "육아", "양육", "난임"
            );
            case VEHICLE_PURCHASE -> rules(
                    "자동차", "차량구입", "차량구매", "구입비", "교통"
            );
            case MONTHLY_RENT -> rules(
                    "월세", "전월세", "임차료", "임대료", "주거비", "주거지원"
            );
            case JEONSE -> rules(
                    "전세", "전월세", "임차보증금", "전세보증금", "보증금", "임대차"
            );
            case HOME_PURCHASE -> rules(
                    "주택구입", "주택구매", "내집마련", "주택자금", "보금자리", "주거지원"
            );
            case REPAYMENT -> rules(
                    "대출상환", "채무", "상환", "신용회복", "재기지원", "대환"
            );
        };
    }

    private List<KeywordRule> rules(String... keywords) {
        List<KeywordRule> result = new ArrayList<>();
        for (int index = 0; index < keywords.length; index++) {
            int titleScore = index < 2 ? 30 : 24;
            int detailScore = index < 2 ? 15 : 12;
            result.add(new KeywordRule(keywords[index], titleScore, detailScore));
        }
        return List.copyOf(result);
    }

    private record KeywordRule(String keyword, int titleScore, int detailScore) {
    }

    private LifecycleSupportDto toLifecycleSupport(
            WelfareSupportProduct product,
            com.via.shinvia.lifecycle.recommendation.dto.LifecycleUserProfileContext userContext
    ) {
        String status = "NEEDS_CONFIRMATION";
        String reason = "심사 필요";
        if (lifecycleEligibilityService != null) {
            var evalResult = lifecycleEligibilityService.evaluateWelfare(product, userContext);
            status = evalResult.status();
            reason = evalResult.reason();
        }

        String updatedDateStr = product.getSourceUpdatedAt() != null 
                ? product.getSourceUpdatedAt().toLocalDate().toString()
                : (product.getUpdatedAt() != null ? product.getUpdatedAt().toLocalDate().toString() : "2026-08-01");

        return LifecycleSupportDto.builder()
                .welfareSupportProductId(product.getWelfareSupportProductId())
                .supportName(product.getProductName())
                .effectType(resolveEffectType(product))
                .amount(null)
                .durationMonths(null)
                .recommendationStatus(status)
                .eligibilityReason(reason)
                .sourceName(firstNotBlank(
                        product.getInstitutionName(),
                        product.getResponsibleInstitution(),
                        "복지로 / 공공데이터포털"
                ))
                .sourceUpdatedAt(updatedDateStr)
                .sourceUrl(product.getRelatedUrl())
                .build();
    }

    private SupportEffectType resolveEffectType(
            WelfareSupportProduct product
    ) {
        String text = String.join(" ",
                value(product.getWelfareType()),
                value(product.getSupportMethod()),
                value(product.getSupportCycle()),
                value(product.getSupportContent())
        );

        if (text.contains("대출") || text.contains("융자")) {
            return SupportEffectType.LOAN;
        }

        if (text.contains("월") || text.contains("매월")) {
            return SupportEffectType.MONTHLY_CASH_INFLOW;
        }

        if (text.contains("바우처") || text.contains("이용권")) {
            return SupportEffectType.VOUCHER;
        }

        if (text.contains("감면") || text.contains("공제")) {
            return SupportEffectType.TAX_BENEFIT;
        }

        return SupportEffectType.CASH_INFLOW;
    }

    private String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return "";
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
