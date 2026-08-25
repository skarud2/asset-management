package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioResultRecord;
import com.via.shinvia.lifecycle.survey.dto.LifecycleBaseSurveyResponse;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LifecycleSimulationService {

    private static final DecimalFormat REPORT_MONEY = new DecimalFormat("#,##0");

    private final LifecycleSurveyService lifecycleSurveyService;
    private final LifecycleProjectionService lifecycleProjectionService;
    private final LifecycleEventInputAssemblerService lifecycleEventInputAssemblerService;
    private final LifecycleEventSequenceService lifecycleEventSequenceService;
    private final LifecycleScenarioResultMapperService lifecycleScenarioResultMapperService;
    private final com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper lifecycleScenarioMapper;
    private final com.via.shinvia.finprofile.FinancialProfileMapper financialProfileMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = createObjectMapper();

    private static com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public LifecycleScenarioResultDto simulate(
            Long userId,
            String loginEmail,
            Long scenarioId,
            LifecycleBaseStateDto baseState
    ) {
        LifecycleBaseSurveyResponse baseSurvey =
                lifecycleSurveyService.getBaseSurvey(userId);

        LifecycleBaseStateDto mergedBaseState =
                mergeBaseSurvey(userId, baseState, baseSurvey);

        LifecycleFinancialStateDto initialState =
                lifecycleProjectionService.createInitialState(mergedBaseState);

        List<LifecycleEventInput> inputs =
                lifecycleEventInputAssemblerService.assembleScenario(
                        userId,
                        loginEmail,
                        scenarioId
                );

        List<LifecycleEventResult> eventResults =
                lifecycleEventSequenceService.execute(
                        initialState,
                        inputs,
                        mergedBaseState.getAnnualSalaryGrowthRate(),
                        null
                );

        LifecycleScenarioResultDto result = lifecycleScenarioResultMapperService.toScenarioResult(
                scenarioId,
                userId,
                initialState,
                inputs,
                eventResults
        );

        // 8. 시뮬레이션 결과 영속화 (DB에 result_data 컬럼이 존재할 때만 안전하게 저장)
        saveResultSafely(scenarioId, userId, result);

        return result;
    }

    private void saveResultSafely(Long scenarioId, Long userId, LifecycleScenarioResultDto result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            lifecycleScenarioMapper.updateSimulationResult(scenarioId, userId, resultJson);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(LifecycleSimulationService.class)
                    .warn("[LifecycleSimulationService] Failed to persist simulation result (table may need result_data column): {}", t.getMessage());
        }
    }

    @Transactional
    public Long completeSimulationResult(Long userId, Long scenarioId) {
        LifecycleScenarioResultDto result = getSimulationResult(userId, scenarioId);
        if (result == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "저장할 시뮬레이션 결과가 없습니다. 시뮬레이션을 먼저 실행해 주세요."
            );
        }
        ensureResultStorage();
        lifecycleScenarioMapper.insertSimulationResultHistory(
                scenarioId,
                userId,
                result,
                toJson(buildOrderedExpenseAmounts(result)),
                toJson(buildOrderedEventCosts(result)),
                toJson(buildOneTimeCostBreakdown(result)),
                toJson(buildMonthlyExpenseBreakdown(result)),
                toJson(buildDetailedAnalysis(result))
        );
        lifecycleScenarioMapper.markScenarioCompleted(scenarioId, userId);
        LifecycleScenarioResultRecord saved =
                lifecycleScenarioMapper.findSimulationResultRecordByScenarioId(scenarioId, userId);
        return saved != null ? saved.getLifecycleScenarioResultId() : null;
    }

    @Transactional
    public List<LifecycleScenarioResultRecord> getSavedResults(Long userId) {
        ensureResultStorage();
        return lifecycleScenarioMapper.findSimulationResultRecordsByUserId(userId);
    }

    @Transactional
    public void deleteSavedResult(Long userId, Long resultId) {
        ensureResultStorage();
        LifecycleScenarioResultRecord record =
                lifecycleScenarioMapper.findSimulationResultRecordById(resultId, userId);
        if (record == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "저장된 시나리오 결과를 찾을 수 없습니다."
            );
        }
        lifecycleScenarioMapper.deleteSimulationResultByScenarioIdAndUserId(
                record.getLifecycleScenarioId(), userId
        );
        lifecycleScenarioMapper.clearSimulationResult(
                record.getLifecycleScenarioId(), userId
        );
    }

    @Transactional
    public LifecycleScenarioResultDto getSavedResult(Long userId, Long resultId) {
        ensureResultStorage();
        LifecycleScenarioResultRecord record =
                lifecycleScenarioMapper.findSimulationResultRecordById(resultId, userId);
        if (record == null) return null;
        return getSimulationResult(userId, record.getLifecycleScenarioId());
    }

    public LifecycleScenarioResultDto getSimulationResult(Long userId, Long scenarioId) {
        try {
            String json = lifecycleScenarioMapper.findSimulationResult(scenarioId, userId);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, LifecycleScenarioResultDto.class);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(LifecycleSimulationService.class)
                    .warn("[LifecycleSimulationService] Failed to read simulation result: {}", t.getMessage());
            return null;
        }
    }

    private void ensureResultStorage() {
        lifecycleScenarioMapper.ensureSimulationResultTable();
        ensureColumn("ordered_expense_amounts_json", lifecycleScenarioMapper::addOrderedExpenseAmountsColumn);
        ensureColumn("ordered_event_costs_json", lifecycleScenarioMapper::addOrderedEventCostsColumn);
        ensureColumn("one_time_cost_breakdown_json", lifecycleScenarioMapper::addOneTimeCostBreakdownColumn);
        ensureColumn("monthly_expense_breakdown_json", lifecycleScenarioMapper::addMonthlyExpenseBreakdownColumn);
        ensureColumn("detailed_analysis_json", lifecycleScenarioMapper::addDetailedAnalysisColumn);
    }

    private void ensureColumn(String columnName, java.util.function.IntSupplier addColumn) {
        try {
            if (lifecycleScenarioMapper.countSimulationResultColumn(columnName) == 0) {
                addColumn.getAsInt();
            }
        } catch (RuntimeException exception) {
            if (lifecycleScenarioMapper.countSimulationResultColumn(columnName) == 0) throw exception;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("금융 라이프 플랜 보고서 데이터를 저장할 수 없습니다.", exception);
        }
    }

    private List<Map<String, String>> buildOrderedEventCosts(LifecycleScenarioResultDto result) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (result.getEventSnapshots() == null) return rows;
        result.getEventSnapshots().stream()
                .sorted(java.util.Comparator.comparing(snapshot -> snapshot.getEventOrder() != null ? snapshot.getEventOrder() : Integer.MAX_VALUE))
                .forEach(snapshot -> rows.add(reportRow(
                        "STEP " + safeOrder(snapshot.getEventOrder()) + " · " + eventLabel(snapshot.getEventType())
                                + (snapshot.getEventDate() != null ? " · " + snapshot.getEventDate() : ""),
                        money(snapshot.getEventCost())
                )));
        return rows;
    }

    private List<Map<String, String>> buildOneTimeCostBreakdown(LifecycleScenarioResultDto result) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (result.getEventSnapshots() == null) return rows;
        result.getEventSnapshots().stream()
                .sorted(java.util.Comparator.comparing(snapshot -> snapshot.getEventOrder() != null ? snapshot.getEventOrder() : Integer.MAX_VALUE))
                .forEach(snapshot -> {
                    String prefix = "STEP " + safeOrder(snapshot.getEventOrder()) + " · " + eventLabel(snapshot.getEventType());
                    int before = rows.size();
                    if (snapshot.getEventType() == com.via.shinvia.lifecycle.common.model.LifecycleEventType.MARRIAGE) {
                        if (snapshot.getLifestyleLevel() == com.via.shinvia.lifecycle.common.model.LifestyleLevel.CUSTOM) {
                            addMoneyRow(rows, prefix + " · 직접 입력 결혼비용", snapshot.getEstimatedCost());
                        } else {
                            addMoneyRow(rows, prefix + " · 예식장·스드메", snapshot.getMarriageHallCost());
                            addMoneyRow(rows, prefix + " · 식대", snapshot.getMarriageMealCost());
                            addMoneyRow(rows, prefix + " · 혼수 준비비", snapshot.getMarriageFurnitureCost());
                            addMoneyRow(rows, prefix + " · 신혼여행 경비", snapshot.getMarriageHoneymoonCost());
                        }
                    } else if (snapshot.getEventType() == com.via.shinvia.lifecycle.common.model.LifecycleEventType.CHILDBIRTH) {
                        addMoneyRow(rows, prefix + " · 산후조리", snapshot.getPostpartumCareCost());
                        addMoneyRow(rows, prefix + " · 카시트", snapshot.getInfantCarSeatCost());
                        addMoneyRow(rows, prefix + " · 유모차", snapshot.getInfantStrollerCost());
                        addMoneyRow(rows, prefix + " · 아기침대", snapshot.getInfantCribCost());
                        addMoneyRow(rows, prefix + " · 기타 준비물", snapshot.getInfantOtherSetupCost());
                    } else {
                        addMoneyRow(rows, prefix + " · 자산가격", snapshot.getAcquiredAssetAmount());
                        addMoneyRow(rows, prefix + " · 취득세·세금", snapshot.getTaxAmount());
                        addMoneyRow(rows, prefix + " · 등기비", snapshot.getRegistrationFeeAmount());
                        addMoneyRow(rows, prefix + " · 중개보수", snapshot.getBrokerageFeeAmount());
                    }
                    if (rows.size() == before) {
                        addMoneyRow(rows, prefix + " · 이벤트 비용", snapshot.getEventCost());
                    }
                });
        return rows;
    }

    private List<Map<String, String>> buildMonthlyExpenseBreakdown(LifecycleScenarioResultDto result) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (result.getEventSnapshots() == null) return rows;
        result.getEventSnapshots().stream()
                .sorted(java.util.Comparator.comparing(snapshot -> snapshot.getEventOrder() != null ? snapshot.getEventOrder() : Integer.MAX_VALUE))
                .forEach(snapshot -> {
                    String prefix = "STEP " + safeOrder(snapshot.getEventOrder()) + " · " + eventLabel(snapshot.getEventType());
                    addMoneyRow(rows, prefix + " · 생활·관리비", snapshot.getAdditionalMonthlyExpense());
                    addMoneyRow(rows, prefix + " · 원금 상환", snapshot.getMonthlyLoanPrincipal());
                    addMoneyRow(rows, prefix + " · 이자 납부", snapshot.getMonthlyLoanInterest());
                });
        return rows;
    }

    private List<Map<String, String>> buildOrderedExpenseAmounts(LifecycleScenarioResultDto result) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (result.getEventSnapshots() == null) return rows;
        result.getEventSnapshots().stream()
                .sorted(java.util.Comparator.comparing(snapshot -> snapshot.getEventOrder() != null ? snapshot.getEventOrder() : Integer.MAX_VALUE))
                .forEach(snapshot -> {
                    BigDecimal recurringExpense = snapshot.getAdditionalMonthlyExpense() != null
                            ? snapshot.getAdditionalMonthlyExpense() : BigDecimal.ZERO;
                    BigDecimal loanPayment = snapshot.getNewLoanMonthlyPayment() != null
                            ? snapshot.getNewLoanMonthlyPayment() : BigDecimal.ZERO;
                    rows.add(reportRow(
                            "STEP " + safeOrder(snapshot.getEventOrder()) + " · " + eventLabel(snapshot.getEventType())
                                    + (snapshot.getEventDate() != null ? " · " + snapshot.getEventDate() : ""),
                            money(recurringExpense.add(loanPayment)) + "/월"
                    ));
                });
        return rows;
    }

    private List<Map<String, String>> buildDetailedAnalysis(LifecycleScenarioResultDto result) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (result.getEventSnapshots() != null) {
            result.getEventSnapshots().stream()
                    .sorted(java.util.Comparator.comparing(snapshot -> snapshot.getEventOrder() != null ? snapshot.getEventOrder() : Integer.MAX_VALUE))
                    .forEach(snapshot -> {
                        String prefix = "STEP " + safeOrder(snapshot.getEventOrder()) + " · " + eventLabel(snapshot.getEventType());
                        rows.add(reportRow(prefix + " 분석", hasText(snapshot.getSummary()) ? snapshot.getSummary() : "저장된 시뮬레이션 결과"));
                        rows.add(reportRow(prefix + " · 세부 산출 내역", ""));
                        if (snapshot.getEventType() == com.via.shinvia.lifecycle.common.model.LifecycleEventType.MARRIAGE) {
                            if (snapshot.getLifestyleLevel() == com.via.shinvia.lifecycle.common.model.LifestyleLevel.CUSTOM) {
                                addMoneyRow(rows, prefix + " · 직접 입력 결혼비용", snapshot.getEstimatedCost());
                            } else {
                                addMoneyRow(rows, prefix + " · 예식장·스드메", snapshot.getMarriageHallCost());
                                addMoneyRow(rows, prefix + " · 식대", snapshot.getMarriageMealCost());
                                addMoneyRow(rows, prefix + " · 혼수 준비비", snapshot.getMarriageFurnitureCost());
                                addMoneyRow(rows, prefix + " · 신혼여행 경비", snapshot.getMarriageHoneymoonCost());
                            }
                        } else if (snapshot.getEventType() == com.via.shinvia.lifecycle.common.model.LifecycleEventType.CHILDBIRTH) {
                            addMoneyRow(rows, prefix + " · 산후조리", snapshot.getPostpartumCareCost());
                            addMoneyRow(rows, prefix + " · 카시트", snapshot.getInfantCarSeatCost());
                            addMoneyRow(rows, prefix + " · 유모차", snapshot.getInfantStrollerCost());
                            addMoneyRow(rows, prefix + " · 아기침대", snapshot.getInfantCribCost());
                            addMoneyRow(rows, prefix + " · 기타 준비물", snapshot.getInfantOtherSetupCost());
                        }
                        addMoneyRow(rows, prefix + " · 총 필요자금", snapshot.getEstimatedCost() != null ? snapshot.getEstimatedCost() : snapshot.getEventCost());
                        addMoneyRow(rows, prefix + " · 매입·취득 자산가격", snapshot.getAcquiredAssetAmount());
                        addMoneyRow(rows, prefix + " · 취득세·세금", snapshot.getTaxAmount());
                        addMoneyRow(rows, prefix + " · 등기비", snapshot.getRegistrationFeeAmount());
                        addMoneyRow(rows, prefix + " · 중개보수", snapshot.getBrokerageFeeAmount());
                        if (snapshot.getEstimatedCost() != null && snapshot.getAcquiredAssetAmount() != null) {
                            addMoneyRow(rows, prefix + " · 부대비용 필요 현금",
                                    snapshot.getEstimatedCost().subtract(snapshot.getAcquiredAssetAmount()).max(BigDecimal.ZERO));
                        }
                        addMoneyRow(rows, prefix + " · 입력 자기자금", snapshot.getUserContributionAmount());
                        addMoneyRow(rows, prefix + " · 가족 지원금", snapshot.getFamilySupportAmount());
                        addMoneyRow(rows, prefix + " · 본인 필요자금", snapshot.getUserRequiredAmount());
                        addMoneyRow(rows, prefix + " · 공공지원", snapshot.getSupportBenefit());
                        addMoneyRow(rows, prefix + " · 월 지출", snapshot.getAdditionalMonthlyExpense());
                        addMoneyRow(rows, prefix + " · 신규 대출", snapshot.getNewLoanAmount());
                        addMoneyRow(rows, prefix + " · 월 대출상환", snapshot.getNewLoanMonthlyPayment());
                        addMoneyRow(rows, prefix + " · 첫 달 원금 상환액", snapshot.getMonthlyLoanPrincipal());
                        addMoneyRow(rows, prefix + " · 첫 달 이자 납부액", snapshot.getMonthlyLoanInterest());
                        if (hasText(snapshot.getLoanRepaymentType()) || snapshot.getNewLoanMonthlyPayment() != null) {
                            rows.add(reportRow(prefix + " · 상환방식 / 월 납입액",
                                    repaymentLabel(snapshot.getLoanRepaymentType()) + " · " + money(snapshot.getNewLoanMonthlyPayment()) + "/월"));
                        }
                        if (snapshot.getLoanPeriodMonths() != null || snapshot.getLoanInterestRate() != null) {
                            rows.add(reportRow(prefix + " · 대출기간 / 적용금리",
                                    (snapshot.getLoanPeriodMonths() != null ? snapshot.getLoanPeriodMonths() + "개월" : "-")
                                            + " · " + percent(snapshot.getLoanInterestRate())));
                        }
                        addEqualPrincipalChartRows(rows, prefix, snapshot);
                        addMoneyRow(rows, prefix + " · 순자산 변화", snapshot.getNetAssetChange());
                        addMoneyRow(rows, prefix + " · 월 저축여력 변화", snapshot.getMonthlySavingCapacityChange());
                        if (snapshot.getAfterDsr() != null) {
                            addTextRow(rows, prefix + " · 이벤트 후 DSR", percent(snapshot.getAfterDsr()));
                        }
                        if (snapshot.getBeforeDsr() != null && snapshot.getAfterDsr() != null) {
                            addTextRow(rows, prefix + " · DSR 변화",
                                    percent(snapshot.getBeforeDsr()) + " → " + percent(snapshot.getAfterDsr()));
                        }
                        if (snapshot.getFeasibility() != null) {
                            rows.add(reportRow(prefix + " · PLAN CHECK", ""));
                            addTextRow(rows, prefix + " · 진행 가능 여부", snapshot.getFeasibility().getTitle());
                            addTextRow(rows, prefix + " · 계획 진단", snapshot.getFeasibility().getMessage());
                            addMoneyRow(rows, prefix + " · 부족 현금", snapshot.getFeasibility().getCashGap());
                            if (snapshot.getFeasibility().getRecommendedDelayMonths() != null) {
                                rows.add(reportRow(prefix + " · 권장 연기 기간", snapshot.getFeasibility().getRecommendedDelayMonths() + "개월"));
                            }
                        }
                        if (snapshot.getSupports() != null && !snapshot.getSupports().isEmpty()) {
                            rows.add(reportRow(prefix + " · 맞춤 복지 혜택", ""));
                            snapshot.getSupports().forEach(support -> {
                                String supportPrefix = prefix + " · 복지 " + nullSafe(support.getSupportName());
                                rows.add(reportRow(supportPrefix, recommendationStatusLabel(support.getRecommendationStatus())));
                                addTextRow(rows, supportPrefix + " · 기관", support.getSourceName());
                                addTextRow(rows, supportPrefix + " · 기준일", support.getSourceUpdatedAt());
                                addTextRow(rows, supportPrefix + " · 판정 사유", support.getEligibilityReason());
                                addMoneyRow(rows, supportPrefix + " · 지원 금액", support.getAmount());
                            });
                        }
                        if (snapshot.getRecommendedProducts() != null && !snapshot.getRecommendedProducts().isEmpty()) {
                            rows.add(reportRow(prefix + " · 추천 금융상품", ""));
                            snapshot.getRecommendedProducts().forEach(product -> {
                                String productPrefix = prefix + " · 상품 " + nullSafe(product.getProductName());
                                rows.add(reportRow(productPrefix, recommendationStatusLabel(product.getRecommendationStatus())));
                                addTextRow(rows, productPrefix + " · 금융기관", product.getInstitutionName());
                                addTextRow(rows, productPrefix + " · 상품유형", product.getProductType());
                                addTextRow(rows, productPrefix + " · 기준일", product.getSourceUpdatedAt());
                                addTextRow(rows, productPrefix + " · 판정 사유", product.getEligibilityReason());
                                addTextRow(rows, productPrefix + " · 금리", product.getInterestRate());
                                addTextRow(rows, productPrefix + " · 한도", product.getLoanLimit());
                                addTextRow(rows, productPrefix + " · 기간", product.getLoanPeriod());
                                addTextRow(rows, productPrefix + " · 상환방식", product.getRepaymentMethod());
                            });
                        }
                    });
        }
        return rows;
    }

    private Map<String, String> reportRow(String label, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        return row;
    }

    private void addMoneyRow(List<Map<String, String>> rows, String label, BigDecimal value) {
        if (value != null && value.signum() != 0) rows.add(reportRow(label, money(value)));
    }

    private void addTextRow(List<Map<String, String>> rows, String label, String value) {
        if (hasText(value)) rows.add(reportRow(label, value));
    }

    private String repaymentLabel(String value) {
        if (!hasText(value)) return "-";
        return switch (value) {
            case "EQUAL_PRINCIPAL" -> "원금균등상환";
            case "EQUAL_PAYMENT" -> "원리금균등상환";
            case "BULLET" -> "만기일시상환";
            default -> value;
        };
    }

    private String recommendationStatusLabel(String value) {
        if (!hasText(value)) return "-";
        return switch (value) {
            case "ELIGIBLE" -> "신청 가능";
            case "NEEDS_CONFIRMATION" -> "확인 필요";
            case "NOT_ELIGIBLE" -> "신청 어려움";
            default -> value;
        };
    }

    private void addEqualPrincipalChartRows(
            List<Map<String, String>> rows,
            String prefix,
            com.via.shinvia.lifecycle.scenario.dto.LifecycleEventSnapshotDto snapshot
    ) {
        if (!"EQUAL_PRINCIPAL".equals(snapshot.getLoanRepaymentType())
                || (snapshot.getEventType() != com.via.shinvia.lifecycle.common.model.LifecycleEventType.JEONSE
                && snapshot.getEventType() != com.via.shinvia.lifecycle.common.model.LifecycleEventType.HOME_PURCHASE)
                || snapshot.getNewLoanAmount() == null
                || snapshot.getNewLoanAmount().signum() <= 0
                || snapshot.getLoanPeriodMonths() == null
                || snapshot.getLoanPeriodMonths() <= 0) return;

        BigDecimal principal = snapshot.getNewLoanAmount();
        BigDecimal monthlyPrincipal = snapshot.getMonthlyLoanPrincipal() != null
                ? snapshot.getMonthlyLoanPrincipal()
                : principal.divide(BigDecimal.valueOf(snapshot.getLoanPeriodMonths()), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal firstInterest = snapshot.getMonthlyLoanInterest() != null
                ? snapshot.getMonthlyLoanInterest() : BigDecimal.ZERO;
        BigDecimal monthlyRate = firstInterest.signum() > 0
                ? firstInterest.divide(principal, 12, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int totalYears = (int) Math.ceil(snapshot.getLoanPeriodMonths() / 12.0);
        int[] candidateYears = {1, 5, 10, 15, 20, 25, 30, 35, 40};
        for (int year : candidateYears) {
            if (year > totalYears) continue;
            int month = Math.min((year - 1) * 12 + 1, snapshot.getLoanPeriodMonths());
            BigDecimal remaining = principal.subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(month - 1))).max(BigDecimal.ZERO);
            BigDecimal payment = monthlyPrincipal.add(remaining.multiply(monthlyRate));
            rows.add(reportRow(prefix + " · 원금균등상환 추이 · " + year + "년 차", money(payment) + "/월"));
        }
    }

    private int safeOrder(Integer order) { return order != null ? order : 0; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String nullSafe(String value) { return value != null && !value.isBlank() ? value : "-"; }
    private String statusSuffix(String status) { return hasText(status) ? " · " + status : ""; }
    private String money(BigDecimal value) { return REPORT_MONEY.format(value != null ? value : BigDecimal.ZERO) + "원"; }
    private String signedMoney(BigDecimal value) { BigDecimal safe = value != null ? value : BigDecimal.ZERO; return (safe.signum() > 0 ? "+" : "") + money(safe); }
    private String percent(BigDecimal value) { return (value != null ? value.stripTrailingZeros().toPlainString() : "0") + "%"; }
    private String eventLabel(com.via.shinvia.lifecycle.common.model.LifecycleEventType type) {
        if (type == null) return "이벤트";
        return switch (type) {
            case MARRIAGE -> "결혼";
            case CHILDBIRTH -> "출산";
            case VEHICLE_PURCHASE -> "차량 구매";
            case MONTHLY_RENT -> "월세";
            case JEONSE -> "전세";
            case HOME_PURCHASE -> "내 집 마련";
            case REPAYMENT -> "대출 상환";
        };
    }

    private LifecycleBaseStateDto mergeBaseSurvey(
            Long userId,
            LifecycleBaseStateDto baseState,
            LifecycleBaseSurveyResponse baseSurvey
    ) {
        LifecycleBaseStateDto merged = baseState != null
                ? baseState
                : new LifecycleBaseStateDto();

        merged.setUserId(userId);

        if (merged.getBaseDate() == null) {
            merged.setBaseDate(LocalDate.now());
        }

        if (userId != null) {
            try {
                com.via.shinvia.finprofile.FinancialProfile profile =
                        financialProfileMapper.findFinancialProfileByUserId(userId);
                if (profile != null) {
                    if (merged.getAnnualIncome() == null && profile.getAnnualIncome() != null) {
                        merged.setAnnualIncome(profile.getAnnualIncome());
                    }
                    if (merged.getLiquidAssetAmount() == null && profile.getLiquidAssetAmount() != null) {
                        merged.setLiquidAssetAmount(profile.getLiquidAssetAmount());
                    }
                }
            } catch (Exception e) {
                // Fallback gracefully if financial profile query fails
            }
        }

        if (merged.getAnnualIncome() == null) {
            merged.setAnnualIncome(new java.math.BigDecimal("40000000"));
        }

        if (merged.getLiquidAssetAmount() == null) {
            merged.setLiquidAssetAmount(new java.math.BigDecimal("15000000"));
        }

        if (baseSurvey != null) {
            if (merged.getMonthlyLivingExpense() == null) {
                merged.setMonthlyLivingExpense(baseSurvey.getMonthlyLivingExpense());
            }

            if (merged.getCurrentHousingType() == null) {
                merged.setCurrentHousingType(baseSurvey.getCurrentHousingType());
            }

            if (merged.getMonthlyHousingExpense() == null) {
                merged.setMonthlyHousingExpense(baseSurvey.getMonthlyHousingExpense());
            }

            if (merged.getIndustryCode() == null) {
                merged.setIndustryCode(baseSurvey.getIndustryCode());
            }

            if (merged.getSalaryGrowthScenario() == null) {
                merged.setSalaryGrowthScenario(baseSurvey.getSalaryGrowthScenario());
            }

            if (merged.getAnnualSalaryGrowthRate() == null) {
                merged.setAnnualSalaryGrowthRate(baseSurvey.getCustomSalaryGrowthRate());
            }
        }

        if (merged.getAnnualSalaryGrowthRate() == null) {
            merged.setAnnualSalaryGrowthRate(new java.math.BigDecimal("0.03"));
        }
        if (merged.getMonthlyLivingExpense() == null) {
            merged.setMonthlyLivingExpense(new java.math.BigDecimal("1500000"));
        }
        if (merged.getMonthlyHousingExpense() == null) {
            merged.setMonthlyHousingExpense(java.math.BigDecimal.ZERO);
        }
        if (merged.getCurrentHousingType() == null) {
            merged.setCurrentHousingType("FAMILY");
        }

        return merged;
    }
}
