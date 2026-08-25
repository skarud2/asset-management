package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileMapper;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.recommendation.dto.LifecycleUserProfileContext;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleEligibilityService {

    private static final Pattern AGE_RANGE_PATTERN = Pattern.compile("(\\d{1,2})\\s*세?\\s*~\\s*(\\d{1,2})\\s*세?");
    private static final Pattern MIN_AGE_PATTERN = Pattern.compile("만?\\s*(\\d{1,2})\\s*세?\\s*이상");
    private static final Pattern MAX_AGE_PATTERN = Pattern.compile("만?\\s*(\\d{1,2})\\s*세?\\s*이하");

    private final UserMapper userMapper;
    private final FinancialProfileMapper financialProfileMapper;

    public record EligibilityResult(String status, String reason) {
    }

    /**
     * 사용자 컨텍스트 빌드
     */
    public LifecycleUserProfileContext buildUserContext(
            Long userId,
            String loginEmail,
            String regionSido,
            String regionSigungu,
            LifecycleFinancialStateDto state
    ) {
        User user = null;
        if (userId != null) {
            user = userMapper.findByUserId(userId);
        } else if (loginEmail != null && !loginEmail.isBlank()) {
            user = userMapper.findByLoginEmail(loginEmail);
        }

        FinancialProfile profile = null;
        if (userId != null) {
            profile = financialProfileMapper.findFinancialProfileByUserId(userId);
        } else if (user != null && user.getUserId() != null) {
            profile = financialProfileMapper.findFinancialProfileByUserId(user.getUserId());
        }

        Integer userAge = null;
        LocalDate birthDate = null;
        if (user != null && user.getBirthDate() != null) {
            birthDate = user.getBirthDate();
            userAge = Period.between(birthDate, LocalDate.now()).getYears();
        }

        BigDecimal annualIncome = profile != null && profile.getAnnualIncome() != null
                ? profile.getAnnualIncome()
                : (state != null ? state.getAnnualIncome() : null);

        BigDecimal liquidAsset = profile != null && profile.getLiquidAssetAmount() != null
                ? profile.getLiquidAssetAmount()
                : (state != null ? state.getCashAsset() : null);

        BigDecimal realEstateAsset = state != null && state.getRealEstateAsset() != null
                ? state.getRealEstateAsset()
                : BigDecimal.ZERO;

        boolean isHomeOwner = realEstateAsset.compareTo(BigDecimal.ZERO) > 0
                || (state != null && "OWN".equalsIgnoreCase(state.getCurrentHousingType()));

        return LifecycleUserProfileContext.builder()
                .userId(user != null ? user.getUserId() : userId)
                .loginEmail(user != null ? user.getLoginEmail() : loginEmail)
                .userName(user != null ? user.getUserName() : null)
                .birthDate(birthDate)
                .userAge(userAge)
                .annualIncome(annualIncome)
                .liquidAssetAmount(liquidAsset)
                .realEstateAsset(realEstateAsset)
                .isHomeOwner(isHomeOwner)
                .regionSido(regionSido)
                .regionSigungu(regionSigungu)
                .creditScore(profile != null ? profile.getCreditScore() : null)
                .employmentStatus(profile != null && profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null)
                .build();
    }

    /**
     * 복지 상품(WelfareSupportProduct)에 대한 적격성 평가
     */
    public EligibilityResult evaluateWelfare(WelfareSupportProduct product, LifecycleUserProfileContext user) {
        if (product == null) {
            return new EligibilityResult("NEEDS_CONFIRMATION", "상세 요건 확인 필요");
        }

        List<String> satisfiedReasons = new ArrayList<>();
        List<String> unmetReasons = new ArrayList<>();
        List<String> needConfirmReasons = new ArrayList<>();

        // 1. 나이 조건 검증
        if (hasText(product.getAgeCondition()) && user.getUserAge() != null) {
            String ageCond = product.getAgeCondition();
            int age = user.getUserAge();
            boolean ageMatched = isAgeEligible(ageCond, age);
            if (!ageMatched) {
                unmetReasons.add(String.format("연령 요건 미충족 (현재 만 %d세 / 대상: %s)", age, ageCond));
            } else {
                satisfiedReasons.add(String.format("만 %d세 연령 요건 충족", age));
            }
        }

        // 2. 지역 조건 검증
        if ("BOKJIRO_LOCAL".equals(product.getSourceType())) {
            if (hasText(product.getRegionSido()) && hasText(user.getRegionSido())) {
                if (!product.getRegionSido().replace(" ", "").contains(user.getRegionSido().replace(" ", ""))) {
                    unmetReasons.add(String.format("거주지역 미일치 (희망: %s / 대상: %s)", user.getRegionSido(), product.getRegionSido()));
                } else if (hasText(product.getRegionSigungu()) && hasText(user.getRegionSigungu())) {
                    if (!product.getRegionSigungu().replace(" ", "").contains(user.getRegionSigungu().replace(" ", ""))) {
                        unmetReasons.add(String.format("시·군·구 미일치 (희망: %s / 대상: %s)", user.getRegionSigungu(), product.getRegionSigungu()));
                    } else {
                        satisfiedReasons.add(String.format("%s %s 거주 요건 충족", user.getRegionSido(), user.getRegionSigungu()));
                    }
                } else {
                    satisfiedReasons.add(String.format("%s 거주 요건 충족", user.getRegionSido()));
                }
            }
        }

        // 3. 무주택 요건 검증
        String targetText = value(product.getSupportTarget()) + " " + value(product.getSupportContent());
        if (targetText.contains("무주택") || targetText.contains("무주택자") || targetText.contains("무주택세대주")) {
            if (Boolean.TRUE.equals(user.getIsHomeOwner())) {
                unmetReasons.add("무주택 요건 미충족 (현재 주택 보유 중)");
            } else {
                satisfiedReasons.add("무주택 요건 충족");
            }
        }

        // 4. 소득 기준 검증 (키워드 기반 간이 판정)
        if (targetText.contains("중위소득") || targetText.contains("소득분위") || targetText.contains("기초생활") || targetText.contains("차상위")) {
            needConfirmReasons.add("가구원수별 소득인정액 심사 필요");
        } else if (targetText.contains("연소득") && user.getAnnualIncome() != null) {
            satisfiedReasons.add("소득 요건 확인 완료");
        }

        // 종합 판정
        if (!unmetReasons.isEmpty()) {
            return new EligibilityResult("NOT_ELIGIBLE", String.join(", ", unmetReasons));
        }

        if (!needConfirmReasons.isEmpty()) {
            String combined = satisfiedReasons.isEmpty() 
                    ? String.join(", ", needConfirmReasons)
                    : String.join(", ", satisfiedReasons) + " (" + String.join(", ", needConfirmReasons) + ")";
            return new EligibilityResult("NEEDS_CONFIRMATION", combined);
        }

        if (!satisfiedReasons.isEmpty()) {
            return new EligibilityResult("ELIGIBLE", "신청 가능: " + String.join(", ", satisfiedReasons));
        }

        return new EligibilityResult("ELIGIBLE", "신청 가능: 기본 지원 요건 충족");
    }

    /**
     * 금융 상품(LifecycleProductDto)에 대한 적격성 평가
     */
    public EligibilityResult evaluateProduct(LifecycleProductDto product, LifecycleUserProfileContext user) {
        if (product == null) {
            return new EligibilityResult("NEEDS_CONFIRMATION", "상세 심사 필요");
        }

        List<String> satisfiedReasons = new ArrayList<>();
        List<String> unmetReasons = new ArrayList<>();

        String pName = value(product.getProductName());

        // 청년 전용 상품 판정
        if (pName.contains("청년") && user.getUserAge() != null) {
            int age = user.getUserAge();
            if (age < 19 || age > 39) {
                unmetReasons.add(String.format("청년 연령 미충족 (만 %d세 / 대상: 만 19~39세)", age));
            } else {
                satisfiedReasons.add(String.format("만 %d세 청년 요건 충족", age));
            }
        }

        // 신혼부부 전용 상품 판정
        if (pName.contains("신혼부부")) {
            satisfiedReasons.add("신혼부부 전용 금리 우대 대상");
        }

        // 디딤돌 / 버팀목 등 무주택자 전용 정책대출
        if ((pName.contains("디딤돌") || pName.contains("버팀목") || pName.contains("보금자리")) && Boolean.TRUE.equals(user.getIsHomeOwner())) {
            if (pName.contains("버팀목")) {
                unmetReasons.add("무주택 요건 미충족 (버팀목 대출은 무주택자 대상)");
            }
        }

        // 소득 요건
        if (user.getAnnualIncome() != null && user.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            satisfiedReasons.add("소득 증빙 가능");
        }

        if (!unmetReasons.isEmpty()) {
            return new EligibilityResult("NOT_ELIGIBLE", String.join(", ", unmetReasons));
        }

        if (!satisfiedReasons.isEmpty()) {
            return new EligibilityResult("ELIGIBLE", "신청 가능: " + String.join(", ", satisfiedReasons));
        }

        return new EligibilityResult("ELIGIBLE", "신청 가능: 금융 프로필 기반 추천");
    }

    private boolean isAgeEligible(String ageCond, int age) {
        if (!hasText(ageCond)) return true;

        Matcher rangeMatcher = AGE_RANGE_PATTERN.matcher(ageCond);
        if (rangeMatcher.find()) {
            int min = Integer.parseInt(rangeMatcher.group(1));
            int max = Integer.parseInt(rangeMatcher.group(2));
            return age >= min && age <= max;
        }

        Matcher minMatcher = MIN_AGE_PATTERN.matcher(ageCond);
        if (minMatcher.find()) {
            int min = Integer.parseInt(minMatcher.group(1));
            return age >= min;
        }

        Matcher maxMatcher = MAX_AGE_PATTERN.matcher(ageCond);
        if (maxMatcher.find()) {
            int max = Integer.parseInt(maxMatcher.group(1));
            return age <= max;
        }

        if (ageCond.contains("청년")) {
            return age >= 19 && age <= 39;
        }

        return true;
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    private String value(String str) {
        return str != null ? str : "";
    }
}
