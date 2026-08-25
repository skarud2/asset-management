package com.via.shinvia.report.futuresim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.via.shinvia.futuresim.entity.FuturesimPlanSnapshot;
import com.via.shinvia.futuresim.service.ComboSimulationService;
import com.via.shinvia.futuresim.service.FutureSimulationEngine;
import com.via.shinvia.futuresim.service.LeverIntensityCalculator;
import com.via.shinvia.futuresim.service.LeverLoanComparisonService;
import com.via.shinvia.futuresim.service.PlanSnapshotService;
import com.via.shinvia.report.futuresim.dto.FuturesimPlanPrintData;
import com.via.shinvia.report.service.provider.ReportCardDataProvider;
import com.via.shinvia.report.service.provider.ReportCardDataProvider.CardData.DetailRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FuturesimCardProvider implements ReportCardDataProvider {

    private static final String CARD_KEY = "FUTURESIM";
    private static final DateTimeFormatter SAVED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private static final Map<String, String> LEVER_LABELS = Map.of(
            "INCOME_CHANGE", "월 추가 확보",
            "LOAN_PREPAYMENT", "대출 조기상환",
            "LOAN_TERM_EXTENSION", "만기 연장",
            "NEW_LOAN", "신규 대출 실행"
    );

    private static final Map<String, String> LEVER_ICONS = Map.of(
            "INCOME_CHANGE", "trending_up",
            "LOAN_PREPAYMENT", "payments",
            "LOAN_TERM_EXTENSION", "event_repeat",
            "NEW_LOAN", "add_card"
    );

    private static final Map<String, String> LOAN_TYPE_LABELS = Map.of(
            "MORTGAGE_LOAN", "주택담보대출",
            "CREDIT_LOAN", "신용대출",
            "JEONSE_LOAN", "전세자금대출",
            "STUDENT_LOAN", "학자금대출"
    );

    private final PlanSnapshotService planSnapshotService;
    private final FutureSimulationEngine futureSimulationEngine;
    private final ComboSimulationService comboSimulationService;
    private final LeverLoanComparisonService leverLoanComparisonService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FuturesimCardProvider(
            PlanSnapshotService planSnapshotService,
            FutureSimulationEngine futureSimulationEngine,
            ComboSimulationService comboSimulationService,
            LeverLoanComparisonService leverLoanComparisonService
    ) {
        this.planSnapshotService = planSnapshotService;
        this.futureSimulationEngine = futureSimulationEngine;
        this.comboSimulationService = comboSimulationService;
        this.leverLoanComparisonService = leverLoanComparisonService;
    }

    @Override
    public String getCardKey() {
        return CARD_KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CardData getCardData(Long userId, Long refId) {
        FuturesimPlanSnapshot plan = refId != null
                ? planSnapshotService.getOwned(userId, refId)
                : latestPlan(userId);

        if (plan == null) {
            String note = refId != null
                    ? "요청한 계획을 찾을 수 없어요. 저장된 계획 목록에서 다시 골라주세요."
                    : "4단계에서 실행 계획을 저장하면 여기에 표시돼요.";
            return new CardData(CARD_KEY, "미래 금융 시뮬레이터", "저장된 계획 없음", "-", List.of(), note, null,null);
        }

        List<LeverEntry> levers = parseLevers(plan.getSelectedLeversJson());
        List<DetailRow> detailRows = new java.util.ArrayList<>();
        detailRows.add(new DetailRow("지금 페이스", formatDuration(plan.getBaselineMonthsToGoal()) + " 후"));
        detailRows.add(new DetailRow("실행 계획 적용", formatDuration(plan.getProjectedMonthsToGoal()) + " 후"));
        detailRows.add(new DetailRow("목표 금액", formatWon(plan.getGoalAmount())));
        for (LeverEntry lever : levers) {
            detailRows.add(new DetailRow("적용한 방법 · " + leverLabel(lever), leverIntensity(lever)));
        }
        detailRows.add(new DetailRow("최종 순자산(목표 시점)", formatWon(plan.getFinalNetWorth())));

        return new CardData(
                CARD_KEY,
                "미래 금융 시뮬레이터",
                plan.getPlanName(),
                compareBadge(plan.getDiffMonths()),
                detailRows,
                null,
                toPrintData(userId, plan, levers),
                null
        );
    }

    private FuturesimPlanPrintData toPrintData(Long userId, FuturesimPlanSnapshot plan, List<LeverEntry> levers) {
        ProjectionData projection = projectionFor(userId, plan, levers);
        return new FuturesimPlanPrintData(
                plan.getPlanName(),
                formatSavedAt(plan.getUpdatedAt()),
                formatWon(plan.getGoalAmount()),
                projection.currentNetWorth(),
                formatDuration(plan.getBaselineMonthsToGoal()),
                formatDuration(plan.getProjectedMonthsToGoal()),
                compareBadge(plan.getDiffMonths()),
                formatWon(plan.getFinalNetWorth()),
                formatRate(plan.getAssumedReturnRate()),
                plan.getBenchmarkLabel(),
                formatWon(plan.getBenchmarkMedianNetWorth()),
                levers.stream().map(lever -> toPrintLever(userId, plan, lever)).toList(),
                parseLoanImpacts(plan.getLoanImpactJson()).stream().map(this::toPrintLoanImpact).toList(),
                projection.baselineTimeline(),
                projection.comboTimeline(),
                projection.benchmarkCrossMonth()
        );
    }

    private ProjectionData projectionFor(Long userId, FuturesimPlanSnapshot plan, List<LeverEntry> levers) {
        try {
            List<LeverIntensityCalculator.LeverSelection> selections = toSelections(levers);
            FutureSimulationEngine.Projection baseline = futureSimulationEngine.calculateProjection(
                    userId, plan.getGoalAmount(), plan.getAssumedReturnRate());
            ComboSimulationService.ComboResult combo = comboSimulationService.simulate(
                    userId, plan.getGoalAmount(), selections, plan.getAssumedReturnRate());
            List<FuturesimPlanPrintData.TimelinePoint> baselineTimeline = toTimeline(baseline.timeline());
            List<FuturesimPlanPrintData.TimelinePoint> comboTimeline = toTimeline(combo.timeline());
            String currentNetWorth = baseline.timeline().isEmpty()
                    ? "-"
                    : formatWon(baseline.timeline().get(0).netWorth());
            return new ProjectionData(
                    currentNetWorth,
                    baselineTimeline,
                    comboTimeline,
                    findCrossMonth(combo.timeline(), plan.getBenchmarkMedianNetWorth())
            );
        } catch (Exception e) {
            return new ProjectionData("-", List.of(), List.of(), null);
        }
    }

    private List<LeverIntensityCalculator.LeverSelection> toSelections(List<LeverEntry> levers) {
        return levers.stream()
                .map(lever -> new LeverIntensityCalculator.LeverSelection(
                        LeverIntensityCalculator.LeverType.valueOf(lever.leverType()), lever.intensity()))
                .toList();
    }

    private List<FuturesimPlanPrintData.TimelinePoint> toTimeline(List<FutureSimulationEngine.TimelinePoint> timeline) {
        return timeline.stream()
                .map(point -> new FuturesimPlanPrintData.TimelinePoint(
                        point.monthOffset(),
                        point.netWorth().toPlainString(),
                        point.contributionAmount().toPlainString(),
                        point.returnAmount().toPlainString()
                ))
                .toList();
    }

    private Integer findCrossMonth(List<FutureSimulationEngine.TimelinePoint> timeline, BigDecimal target) {
        if (target == null) return null;
        return timeline.stream()
                .filter(point -> point.netWorth().compareTo(target) >= 0)
                .map(FutureSimulationEngine.TimelinePoint::monthOffset)
                .findFirst()
                .orElse(null);
    }

    private FuturesimPlanPrintData.Lever toPrintLever(Long userId, FuturesimPlanSnapshot plan, LeverEntry lever) {
        return new FuturesimPlanPrintData.Lever(
                LEVER_ICONS.getOrDefault(lever.leverType(), "check_circle"),
                leverLabel(lever),
                leverIntensity(lever),
                individualEffect(userId, plan, lever),
                financialEffect(userId, plan, lever)
        );
    }

    private String financialEffect(Long userId, FuturesimPlanSnapshot plan, LeverEntry lever) {
        if ("INCOME_CHANGE".equals(lever.leverType())) {
            BigDecimal cumulative = safe(lever.intensity()).multiply(BigDecimal.valueOf(Math.max(0, plan.getProjectedMonthsToGoal())));
            return "매달 여유자금 +" + formatWon(lever.intensity()) + " · 목표까지 누적 +" + formatWon(cumulative);
        }
        try {
            LeverIntensityCalculator.LeverType type = LeverIntensityCalculator.LeverType.valueOf(lever.leverType());
            LeverLoanComparisonService.Summary baseline = leverLoanComparisonService.baseline(userId);
            LeverLoanComparisonService.Summary adjusted = leverLoanComparisonService.forLever(userId, type, lever.intensity());
            BigDecimal monthlyDiff = baseline.monthlyBurden().subtract(adjusted.monthlyBurden());
            BigDecimal interestDiff = adjusted.totalInterest().subtract(baseline.totalInterest());
            String monthly = monthlyDiff.signum() == 0 ? null
                    : "월 상환액 " + formatWon(monthlyDiff.abs()) + (monthlyDiff.signum() > 0 ? " 절감" : " 증가");
            String interest = interestDiff.signum() == 0 ? null
                    : "총이자 " + formatWon(interestDiff.abs()) + (interestDiff.signum() > 0 ? " 증가" : " 감소");
            return joinEffects(monthly, interest);
        } catch (Exception e) {
            return null;
        }
    }

    private String joinEffects(String first, String second) {
        if (first == null) return second;
        if (second == null) return first;
        return first + " · " + second;
    }

    private String individualEffect(Long userId, FuturesimPlanSnapshot plan, LeverEntry lever) {
        try {
            LeverIntensityCalculator.LeverSelection selection = new LeverIntensityCalculator.LeverSelection(
                    LeverIntensityCalculator.LeverType.valueOf(lever.leverType()), lever.intensity());
            Integer diffMonths = comboSimulationService.simulate(
                    userId, plan.getGoalAmount(), List.of(selection), plan.getAssumedReturnRate()).diffMonths();
            if (diffMonths == null || diffMonths == 0) return null;
            return diffMonths > 0 ? formatDuration(diffMonths) + " 단축" : formatDuration(Math.abs(diffMonths)) + " 지연";
        } catch (Exception e) {
            return null;
        }
    }

    private String leverLabel(LeverEntry lever) {
        return LEVER_LABELS.getOrDefault(lever.leverType(), lever.leverType());
    }

    private String leverIntensity(LeverEntry lever) {
        return "LOAN_TERM_EXTENSION".equals(lever.leverType())
                ? formatDuration(lever.intensity() == null ? null : lever.intensity().intValue())
                : formatWon(lever.intensity());
    }

    private FuturesimPlanPrintData.LoanImpact toPrintLoanImpact(LoanImpactEntry impact) {
        BigDecimal saving = safe(impact.beforeMonthlyPayment()).subtract(safe(impact.afterMonthlyPayment()));
        String repaymentPeriod = impact.beforeRemainingMonths() == null || impact.afterRemainingMonths() == null
                ? null
                : formatDuration(impact.beforeRemainingMonths()) + " → " + formatDuration(impact.afterRemainingMonths());
        String totalInterestDiff = impact.totalInterestDiff() == null || impact.totalInterestDiff().signum() == 0
                ? null
                : "총이자 " + (impact.totalInterestDiff().signum() > 0 ? "+" : "") + formatWon(impact.totalInterestDiff());
        String prepaymentFee = impact.prepaymentFeeRate() == null || impact.prepaymentFeeRate().signum() <= 0
                ? null
                : "중도상환수수료 " + impact.prepaymentFeeRate().stripTrailingZeros().toPlainString() + "%"
                + (impact.prepaymentFeeEndDate() == null ? "" : " · " + impact.prepaymentFeeEndDate() + "까지");
        return new FuturesimPlanPrintData.LoanImpact(
                LOAN_TYPE_LABELS.getOrDefault(impact.loanType(), impact.loanType() == null ? "대출 상환 계획" : impact.loanType()),
                formatWon(impact.beforeMonthlyPayment()),
                formatWon(impact.afterMonthlyPayment()),
                saving.signum() > 0 ? "월 " + formatWon(saving) + " 절감" : null,
                repaymentPeriod,
                totalInterestDiff,
                prepaymentFee
        );
    }

    private FuturesimPlanSnapshot latestPlan(Long userId) {
        List<FuturesimPlanSnapshot> plans = planSnapshotService.list(userId);
        return plans.isEmpty() ? null : plans.get(0);
    }

    private String compareBadge(int diffMonths) {
        if (diffMonths == 0) return "변화 없음";
        if (diffMonths > 0) return "-" + formatDuration(diffMonths) + " 단축";
        return "+" + formatDuration(Math.abs(diffMonths)) + " 지연";
    }

    private List<LeverEntry> parseLevers(String selectedLeversJson) {
        if (selectedLeversJson == null || selectedLeversJson.isBlank()) return List.of();
        try {
            LeverEntry[] entries = objectMapper.readValue(selectedLeversJson, LeverEntry[].class);
            return List.of(entries);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<LoanImpactEntry> parseLoanImpacts(String loanImpactJson) {
        if (loanImpactJson == null || loanImpactJson.isBlank()) return List.of();
        try {
            LoanImpactEntry[] entries = objectMapper.readValue(loanImpactJson, LoanImpactEntry[].class);
            return List.of(entries);
        } catch (Exception e) {
            return List.of();
        }
    }

    private record LeverEntry(String leverType, BigDecimal intensity) {
    }

    private record LoanImpactEntry(
            String loanType,
            BigDecimal beforeMonthlyPayment,
            BigDecimal afterMonthlyPayment,
            BigDecimal totalInterestDiff,
            Integer beforeRemainingMonths,
            Integer afterRemainingMonths,
            BigDecimal prepaymentFeeRate,
            String prepaymentFeeEndDate
    ) {
    }

    private record ProjectionData(
            String currentNetWorth,
            List<FuturesimPlanPrintData.TimelinePoint> baselineTimeline,
            List<FuturesimPlanPrintData.TimelinePoint> comboTimeline,
            Integer benchmarkCrossMonth
    ) {
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) return "-";
        return String.format(Locale.KOREA, "%,d원", amount.longValue());
    }

    private String formatDuration(Integer months) {
        if (months == null) return "-";
        int abs = Math.abs(months);
        int years = abs / 12;
        int rest = abs % 12;
        if (years > 0) return rest > 0 ? years + "년 " + rest + "개월" : years + "년";
        return rest + "개월";
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) return "-";
        return rate.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private String formatSavedAt(LocalDateTime updatedAt) {
        return updatedAt == null ? "-" : updatedAt.format(SAVED_AT_FORMATTER);
    }
}
