package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.recommendation.adapter.LoanRecommendationAdapter;
import com.via.shinvia.lifecycle.recommendation.adapter.PolicyRecommendationAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LifecycleProductService {

    private static final int EACH_SOURCE_LIMIT = 3;
    private static final int CHILDBIRTH_CANDIDATE_LIMIT = 20;
    private static final int TOTAL_LIMIT = 5;
    private static final List<String> CHILDBIRTH_SPECIAL_CONDITION_KEYWORDS = List.of(
            "다자녀", "한부모", "미혼모", "미혼부", "입양", "장애", "저소득",
            "기초생활", "차상위", "취약계층", "신혼부부", "농어업인",
            "농업인", "어업인", "군인", "공무원", "교직원"
    );

    private final LoanRecommendationAdapter loanRecommendationAdapter;
    private final PolicyRecommendationAdapter policyRecommendationAdapter;
    private final LifecycleEligibilityService lifecycleEligibilityService;

    public List<LifecycleProductDto> getRecommendedProducts(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType,
            BigDecimal requestedAmount,
            Integer termMonths
    ) {
        List<LifecycleProductDto> products = new ArrayList<>();
        int candidateLimit = eventType == LifecycleEventType.CHILDBIRTH
                ? CHILDBIRTH_CANDIDATE_LIMIT
                : EACH_SOURCE_LIMIT;

        products.addAll(loanRecommendationAdapter.recommend(
                loginEmail,
                eventType,
                requestedAmount,
                termMonths,
                candidateLimit
        ));
        products.addAll(policyRecommendationAdapter.recommend(
                userId,
                eventType,
                candidateLimit
        ));

        var userContext = lifecycleEligibilityService != null
                ? lifecycleEligibilityService.buildUserContext(userId, loginEmail, null, null, null)
                : null;

        return deduplicate(products).stream()
                .filter(product -> isPurposeRelevant(product, eventType))
                .filter(product -> isGenerallyApplicableChildbirthProduct(product, eventType))
                .peek(product -> {
                    String status = "ELIGIBLE";
                    String reason = "추천 상품";
                    if (lifecycleEligibilityService != null) {
                        var evalResult = lifecycleEligibilityService.evaluateProduct(product, userContext);
                        status = evalResult.status();
                        reason = evalResult.reason();
                    }
                    product.setRecommendationStatus(status);
                    product.setEligibilityReason(reason);
                    if (product.getSourceName() == null || product.getSourceName().isBlank()) {
                        product.setSourceName(product.getInstitutionName() != null ? product.getInstitutionName() : "금융감독원 / 서민금융진흥원");
                    }
                    if (product.getSourceUpdatedAt() == null) {
                        product.setSourceUpdatedAt("2026-08-01");
                    }
                })
                .filter(product -> eventType != LifecycleEventType.CHILDBIRTH
                        || !"NOT_ELIGIBLE".equals(product.getRecommendationStatus()))
                .limit(TOTAL_LIMIT)
                .toList();
    }

    public List<LifecycleProductDto> getRecommendedProducts(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType
    ) {
        return getRecommendedProducts(
                userId,
                loginEmail,
                eventType,
                null,
                null
        );
    }

    private List<LifecycleProductDto> deduplicate(
            List<LifecycleProductDto> products
    ) {
        Map<String, LifecycleProductDto> result = new LinkedHashMap<>();

        for (LifecycleProductDto product : products) {
            if (product == null) {
                continue;
            }
            String key = product.getProductType()
                    + ":"
                    + product.getProductId();
            result.putIfAbsent(key, product);
        }

        return new ArrayList<>(result.values());
    }

    private boolean isPurposeRelevant(
            LifecycleProductDto product,
            LifecycleEventType eventType
    ) {
        String name = product.getProductName();
        if (name == null || name.isBlank()) {
            return false;
        }

        String normalizedName = name.replaceAll("\\s+", "");
        List<String> purposeKeywords = switch (eventType) {
            case MARRIAGE -> List.of("결혼", "혼례", "웨딩", "신혼", "부부", "드림", "청년");
            case CHILDBIRTH -> List.of("출산", "육아", "아이", "자녀", "신생아", "부모");
            case VEHICLE_PURCHASE -> List.of("자동차", "오토", "차량", "마이카", "카", "친환경", "드라이브");
            case MONTHLY_RENT -> List.of("월세", "임차료", "전월세", "청년", "주거", "보증금");
            case JEONSE -> List.of("전세", "임차보증금", "전월세보증금", "버팀목", "안심전세", "청년");
            case HOME_PURCHASE -> List.of("주택", "내집", "구입자금", "디딤돌", "모기지", "보금자리", "담보대출", "주담대");
            case REPAYMENT -> List.of("대환", "전환", "갈아타기", "상환", "저금리", "안심전환");
        };

        return purposeKeywords.stream()
                .anyMatch(keyword -> normalizedName.contains(keyword) || (product.getProductType() != null && product.getProductType().contains(keyword)));
    }

    private boolean isGenerallyApplicableChildbirthProduct(
            LifecycleProductDto product,
            LifecycleEventType eventType
    ) {
        if (eventType != LifecycleEventType.CHILDBIRTH || product.getProductName() == null) {
            return true;
        }

        String normalizedName = product.getProductName().replaceAll("\\s+", "");
        return CHILDBIRTH_SPECIAL_CONDITION_KEYWORDS.stream()
                .noneMatch(normalizedName::contains);
    }
}
