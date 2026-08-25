package com.via.shinvia.report.service.provider;

import com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioResultRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Component
public class LifecycleScenarioCardProvider implements ReportCardDataProvider {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");
    private final LifecycleScenarioMapper scenarioMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public LifecycleScenarioCardProvider(LifecycleScenarioMapper scenarioMapper) {
        this.scenarioMapper = scenarioMapper;
    }

    @Override
    public String getCardKey() {
        return "FINANCIAL_CYCLE_PLAN";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CardData getCardData(Long userId, Long refId) {
        try {
            ensureResultStorage();
            LifecycleScenarioResultRecord record = refId != null
                    ? scenarioMapper.findSimulationResultRecordById(refId, userId)
                    : scenarioMapper.findLatestSimulationResultRecord(userId);
            if (record == null) {
                return emptyCard();
            }
            List<CardData.DetailRow> rows = new ArrayList<>();
            rows.add(new CardData.DetailRow("시나리오 순서별 지출 금액", ""));
            rows.addAll(readRows(record.getOrderedExpenseAmountsJson()));
            rows.add(new CardData.DetailRow("월 지출 상세 구성", ""));
            rows.addAll(readRows(record.getMonthlyExpenseBreakdownJson()));
            rows.add(new CardData.DetailRow("시나리오 순서별 소요 비용", ""));
            rows.addAll(readRows(record.getOrderedEventCostsJson()));
            rows.add(new CardData.DetailRow("일회성 비용 상세 구성", ""));
            rows.addAll(readRows(record.getOneTimeCostBreakdownJson()));
            rows.add(new CardData.DetailRow("상세 분석 보고서", ""));
            rows.addAll(readRows(record.getDetailedAnalysisJson()));
            return new CardData(
                    getCardKey(),
                    "금융 라이프 플랜 · " + record.getScenarioName(),
                    "최종 순자산",
                    money(record.getFinalNetAsset()),
                    rows,
                    "저장된 금융 라이프 플랜 결과 기준",
                    null,
                    null
            );
        } catch (Exception ignored) {
            return emptyCard();
        }
    }

    private CardData emptyCard() {
        return new CardData(
                getCardKey(), "금융 라이프 플랜", "최종 순자산", "결과 없음",
                List.of(), "금융 라이프 플랜 결과를 먼저 저장해 주세요.", null, null);
    }

    private void ensureResultStorage() {
        scenarioMapper.ensureSimulationResultTable();
        if (scenarioMapper.countSimulationResultColumn("ordered_expense_amounts_json") == 0) {
            try { scenarioMapper.addOrderedExpenseAmountsColumn(); } catch (RuntimeException ignored) { }
        }
        if (scenarioMapper.countSimulationResultColumn("ordered_event_costs_json") == 0) {
            try { scenarioMapper.addOrderedEventCostsColumn(); } catch (RuntimeException ignored) { }
        }
        if (scenarioMapper.countSimulationResultColumn("one_time_cost_breakdown_json") == 0) {
            try { scenarioMapper.addOneTimeCostBreakdownColumn(); } catch (RuntimeException ignored) { }
        }
        if (scenarioMapper.countSimulationResultColumn("monthly_expense_breakdown_json") == 0) {
            try { scenarioMapper.addMonthlyExpenseBreakdownColumn(); } catch (RuntimeException ignored) { }
        }
        if (scenarioMapper.countSimulationResultColumn("detailed_analysis_json") == 0) {
            try { scenarioMapper.addDetailedAnalysisColumn(); } catch (RuntimeException ignored) { }
        }
    }

    private List<CardData.DetailRow> readRows(String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (json == null || json.isBlank()) return List.of();
        List<Map<String, String>> rows = objectMapper.readValue(
                json,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() { }
        );
        return rows.stream()
                .map(row -> new CardData.DetailRow(row.getOrDefault("label", ""), row.getOrDefault("value", "")))
                .toList();
    }

    private String money(BigDecimal value) {
        return MONEY.format(value != null ? value : BigDecimal.ZERO) + "원";
    }

    private String signedMoney(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return (safe.signum() > 0 ? "+" : "") + money(safe);
    }

    private String percent(BigDecimal value) {
        return (value != null ? value.stripTrailingZeros().toPlainString() : "0") + "%";
    }
}
