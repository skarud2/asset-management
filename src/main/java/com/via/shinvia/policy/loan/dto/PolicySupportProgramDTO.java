package com.via.shinvia.policy.loan.dto;

import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Getter
@Builder
// 맞춤대출 상품정보 전달 기능
public class PolicySupportProgramDTO {

    private Long policySupportProgramId;

    private String externalSeq;

    private String programName;

    private String productBadge;

    private String targetDescription;

    private BigDecimal maxSupportAmount;

    private BigDecimal minInterestRate;

    private BigDecimal maxInterestRate;

    private String interestRateType;

    private String interestRateDescription;

    private String supportPeriodDescription;

    private String repaymentMethod;

    private String usageDescription;

    private String institutionCategory;

    private String offeringInstitutionName;

    private String handlingInstitution;

    private String supportArea;

    private String eligibilityDescription;

    private String applicationMethod;

    private String contactDescription;

    private String applicationUrl;

    private String operationPeriod;

    private Map<String, String> eligibility;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active;

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    public static PolicySupportProgramDTO from(
            PolicySupportProgram entity
    ) {
        Map<String, String> eligibility =
                parseEligibility(entity.getEligibilityJson());

        String rawProductCategory =
                eligibility.get("productCategory");

        if (rawProductCategory != null
                && rawProductCategory.matches("\\d+")) {
            eligibility.remove("productCategory");
        }

        return PolicySupportProgramDTO.builder()

                .policySupportProgramId(
                        entity.getPolicySupportProgramId()
                )

                .externalSeq(
                        entity.getExternalSeq()
                )

                .programName(
                        entity.getProgramName()
                )

                .productBadge(
                        resolveProductBadge(
                                entity.getUsageDescription(),
                                entity.getInstitutionCategory(),
                                rawProductCategory
                        )
                )

                .targetDescription(
                        entity.getTargetDescription()
                )

                .maxSupportAmount(
                        entity.getMaxSupportAmount()
                )

                .minInterestRate(
                        entity.getMinInterestRate()
                )

                .maxInterestRate(
                        entity.getMaxInterestRate()
                )

                .interestRateType(
                        entity.getInterestRateType()
                )

                .interestRateDescription(
                        entity.getInterestRateDescription()
                )

                .supportPeriodDescription(
                        entity.getSupportPeriodDescription()
                )

                .repaymentMethod(
                        entity.getRepaymentMethod()
                )

                .usageDescription(
                        entity.getUsageDescription()
                )

                .institutionCategory(
                        entity.getInstitutionCategory()
                )

                .offeringInstitutionName(
                        entity.getOfferingInstitutionName()
                )

                .handlingInstitution(
                        entity.getHandlingInstitution()
                )

                .supportArea(
                        entity.getSupportArea()
                )

                .eligibilityDescription(
                        entity.getEligibilityDescription()
                )

                .applicationMethod(
                        entity.getApplicationMethod()
                )

                .contactDescription(
                        entity.getContactDescription()
                )

                .applicationUrl(
                        entity.getApplicationUrl()
                )

                .operationPeriod(
                        entity.getOperationPeriod()
                )

                .eligibility(
                        eligibility
                )

                .effectiveFrom(
                        entity.getEffectiveFrom()
                )

                .effectiveTo(
                        entity.getEffectiveTo()
                )

                .active(
                        entity.getActive()
                )

                .build();
    }

    private static String resolveProductBadge(
            String usage,
            String institutionCategory,
            String productCategory
    ) {
        if (usage != null && !usage.isBlank()) {
            return usage;
        }

        if (productCategory != null
                && !productCategory.isBlank()
                && !productCategory.matches("\\d+")) {
            return productCategory;
        }

        if (institutionCategory != null
                && !institutionCategory.isBlank()) {
            return institutionCategory;
        }

        return "대출상품";
    }

    private static Map<String, String> parseEligibility(
            String json
    ) {
        Map<String, String> result =
                new LinkedHashMap<>();

        if (json == null
                || json.isBlank()) {
            return result;
        }

        try {
            JsonNode root =
                    OBJECT_MAPPER.readTree(json);

            root.properties()
                    .forEach(entry -> {

                        JsonNode value =
                                entry.getValue();

                        result.put(
                                entry.getKey(),
                                value == null
                                        ? ""
                                        : value.asText()
                        );
                    });

        } catch (Exception e) {

            log.warn(
                    "정책상품 조건 JSON 파싱 실패: {}",
                    json
            );
        }

        return result;
    }
}
