package com.via.shinvia.policy.loan.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.via.shinvia.policy.loan.dto.api.LoanProductApiItem;
import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.via.shinvia.policy.common.util.PolicyProductValues.url;

@Component
// API 상품정보를 저장 데이터로 변환하는 기능
public class PolicySupportProgramConverter {

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\d+(?:\\.\\d+)?");

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PolicySupportProgram toEntity(
            LoanProductApiItem item
    ) {
        InterestRange interestRange =
                parseInterestRate(
                        item.getInterestRate()
                );

        return PolicySupportProgram.create(
                item.getSeq(),

                defaultValue(
                        item.getFinancialProductName(),
                        "상품명 미등록"
                ),

                firstNotBlank(
                        item.getTargetFilter(),
                        item.getTarget()
                ),

                parseLoanLimit(
                        item.getLoanLimit()
                ),

                interestRange.min(),

                interestRange.max(),

                item.getInterestRateCategory(),

                item.getInterestRate(),

                createPeriodDescription(item),

                item.getRepaymentMethod(),

                item.getUsage(),

                item.getInstitutionCategory(),

                item.getOfferingInstitutionName(),

                item.getHandlingInstitution(),

                item.getSupportArea(),

                item.getSupportTargetDetailCondition(),

                item.getJoinMethod(),

                firstNotBlank(
                        item.getContact(),
                        item.getReferenceContact()
                ),

                normalizeUrl(
                        item.getRelatedSite()
                ),

                item.getProductOperationPeriod(),

                createEligibilityJson(item)
        );
    }

    public void updateEntity(
            PolicySupportProgram entity,
            LoanProductApiItem item
    ) {
        InterestRange interestRange =
                parseInterestRate(
                        item.getInterestRate()
                );

        entity.update(
                defaultValue(
                        item.getFinancialProductName(),
                        "상품명 미등록"
                ),

                firstNotBlank(
                        item.getTargetFilter(),
                        item.getTarget()
                ),

                parseLoanLimit(
                        item.getLoanLimit()
                ),

                interestRange.min(),

                interestRange.max(),

                item.getInterestRateCategory(),

                item.getInterestRate(),

                createPeriodDescription(item),

                item.getRepaymentMethod(),

                item.getUsage(),

                item.getInstitutionCategory(),

                item.getOfferingInstitutionName(),

                item.getHandlingInstitution(),

                item.getSupportArea(),

                item.getSupportTargetDetailCondition(),

                item.getJoinMethod(),

                firstNotBlank(
                        item.getContact(),
                        item.getReferenceContact()
                ),

                normalizeUrl(
                        item.getRelatedSite()
                ),

                item.getProductOperationPeriod(),

                createEligibilityJson(item)
        );
    }

    /**
     * API 대출한도는 만원 단위로 제공된다.
     *
     * 2000 → 20,000,000원
     */
    private BigDecimal parseLoanLimit(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value)
                    .multiply(
                            BigDecimal.valueOf(
                                    10_000
                            )
                    );

        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 금리 문자열에서 숫자를 추출한다.
     *
     * 3
     * → min 3, max 3
     *
     * 3~5
     * → min 3, max 5
     *
     * ~19.99
     * → min null, max 19.99
     *
     * 무이자
     * → min 0, max 0
     */
    private InterestRange parseInterestRate(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return new InterestRange(
                    null,
                    null
            );
        }

        if (value.contains("무이자")) {
            return new InterestRange(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        Matcher matcher =
                NUMBER_PATTERN.matcher(value);

        BigDecimal first = null;
        BigDecimal second = null;

        if (matcher.find()) {
            first = new BigDecimal(
                    matcher.group()
            );
        }

        if (matcher.find()) {
            second = new BigDecimal(
                    matcher.group()
            );
        }

        if (first == null) {
            return new InterestRange(
                    null,
                    null
            );
        }

        if (value.trim().startsWith("~")) {
            return new InterestRange(
                    null,
                    first
            );
        }

        if (second != null) {
            return new InterestRange(
                    first,
                    second
            );
        }

        return new InterestRange(
                first,
                first
        );
    }

    private String createPeriodDescription(
            LoanProductApiItem item
    ) {
        StringBuilder builder =
                new StringBuilder();

        appendPeriod(
                builder,
                "총 대출기간",
                item.getMaxTotalLoanTerm()
        );

        appendPeriod(
                builder,
                "거치기간",
                item.getMaxDeferredTerm()
        );

        appendPeriod(
                builder,
                "상환기간",
                item.getMaxRepaymentTerm()
        );

        if (builder.isEmpty()) {
            return null;
        }

        return builder.toString();
    }

    private void appendPeriod(
            StringBuilder builder,
            String label,
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append(" / ");
        }

        builder
                .append(label)
                .append(": ")
                .append(value);
    }

    private String createEligibilityJson(
            LoanProductApiItem item
    ) {
        try {
            Map<String, Object> conditions =
                    new LinkedHashMap<>();

            put(
                    conditions,
                    "age",
                    item.getAge()
            );

            put(
                    conditions,
                    "income",
                    item.getIncome()
            );

            put(
                    conditions,
                    "residenceArea",
                    item.getResidenceArea()
            );

            put(
                    conditions,
                    "creditScore",
                    item.getCreditScore()
            );

            put(
                    conditions,
                    "householdCondition",
                    item.getHouseholdCondition()
            );

            put(
                    conditions,
                    "guaranteeInstitution",
                    item.getGuaranteeInstitution()
            );

            put(
                    conditions,
                    "repaymentFee",
                    item.getRepaymentFee()
            );

            put(
                    conditions,
                    "loanIncidentalCost",
                    item.getLoanIncidentalCost()
            );

            put(
                    conditions,
                    "overdueInterestRate",
                    item.getOverdueInterestRate()
            );

            put(
                    conditions,
                    "preferentialInterestCondition",
                    item.getPreferentialInterestCondition()
            );

            put(
                    conditions,
                    "etcReference",
                    item.getEtcReference()
            );

            put(
                    conditions,
                    "handlingInstitutionDetail",
                    item.getHandlingInstitutionDetail()
            );

            put(
                    conditions,
                    "productCategory",
                    item.getProductCategory()
            );

            put(
                    conditions,
                    "financialEducationProductYn",
                    item.getFinancialEducationProductYn()
            );

            put(
                    conditions,
                    "financialEducationProductEtc",
                    item.getFinancialEducationProductEtc()
            );

            return objectMapper
                    .writeValueAsString(
                            conditions
                    );

        } catch (Exception e) {
            return "{}";
        }
    }

    private void put(
            Map<String, Object> map,
            String key,
            String value
    ) {
        if (value != null
                && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private String normalizeUrl(
            String value
    ) {
        return url(value);
    }

    private String firstNotBlank(
            String first,
            String second
    ) {
        if (first != null
                && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private String defaultValue(
            String value,
            String defaultValue
    ) {
        if (value == null
                || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    private record InterestRange(
            BigDecimal min,
            BigDecimal max
    ) {
    }
}
