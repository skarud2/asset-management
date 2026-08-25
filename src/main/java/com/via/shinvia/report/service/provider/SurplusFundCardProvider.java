package com.via.shinvia.report.service.provider;

import com.via.shinvia.report.surplusfund.dto.SurplusFundPrintData;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionDetailResponse;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionSummaryResponse;
import com.via.shinvia.surplusfund.guideversion.service.SurplusFundGuideVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SurplusFundCardProvider implements ReportCardDataProvider {

    private static final String CARD_KEY = "SURPLUS_FUND";
    private static final DateTimeFormatter SAVED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final SurplusFundGuideVersionService guideVersionService;

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
        GuideVersionDetailResponse detail = findGuideVersion(userId, refId);

        if (detail == null) {
            return new CardData(
                    CARD_KEY,
                    "여유자금 운용",
                    "저장된 운용 기록 없음",
                    "-",
                    List.of(),
                    "여유자금 운용 가이드에서 운용 기록을 저장하면 여기에 표시돼요.",
                    null,
                    null
            );
        }

        GuideVersionDetailResponse.Calculation calculation = detail.calculation();
        GuideVersionDetailResponse.PlanResult planResult = detail.planResult();

        List<CardData.DetailRow> detailRows = new ArrayList<>();

        detailRows.add(new CardData.DetailRow("투자 성향", investmentStyleLabel(planResult.investmentStyle())));
        detailRows.add(new CardData.DetailRow("현금", findAllocationAmount(planResult.allocations(), "CASH")));
        detailRows.add(new CardData.DetailRow("ETF", findAllocationAmount(planResult.allocations(), "ETF")));
        detailRows.add(new CardData.DetailRow("펀드", findAllocationAmount(planResult.allocations(), "FUND")));

        return new CardData(
                CARD_KEY,
                "여유자금 운용",
                "최종 운용 가능 금액",
                formatWon(calculation.finalSurplusAmount()),
                detailRows,
                null,
                null,
                toPrintData(detail)
        );
    }

    private GuideVersionDetailResponse findGuideVersion(Long userId, Long refId) {
        if (refId != null) {
            return guideVersionService.findDetail(userId, refId);
        }

        List<GuideVersionSummaryResponse> versions = guideVersionService.findAll(userId);

        if (versions.isEmpty()) {
            return null;
        }

        Long latestGuideVersionId = versions.get(0).surplusFundGuideVersionId();

        return guideVersionService.findDetail(userId, latestGuideVersionId);
    }

    private SurplusFundPrintData toPrintData(GuideVersionDetailResponse detail) {
        GuideVersionDetailResponse.Calculation calculation = detail.calculation();
        GuideVersionDetailResponse.PlanResult planResult = detail.planResult();

        List<SurplusFundPrintData.Allocation> allocations = planResult.allocations()
                                                                        .stream()
                                                                        .sorted((a, b) ->
                                                                                Integer.compare(
                                                                                        assetOrder(a.assetType()),
                                                                                        assetOrder(b.assetType())
                                                                                )
                                                                        )
                                                                        .map(allocation -> new SurplusFundPrintData.Allocation(
                                                                                        allocation.assetType(),
                                                                                        assetTypeLabel(allocation.assetType()),
                                                                                        formatRatio(allocation.allocationRatio()),
                                                                                        formatWon(allocation.allocationAmount())
                                                                                )
                                                                        )
                                                                        .toList();

        List<SurplusFundPrintData.Etf> etfs = detail.interestedEtfs().stream()
                                                    .map(etf ->
                                                            new SurplusFundPrintData.Etf(
                                                                    etf.selectionOrder(),
                                                                    etf.productName(),
                                                                    etf.productCode(),
                                                                    etf.priceBaseDate() == null
                                                                            ? "-"
                                                                            : etf.priceBaseDate().toString(),
                                                                    formatWon(etf.closingPrice()),
                                                                    formatRate(etf.fluctuationRate())
                                                            )
                                                    )
                                                    .toList();

        return new SurplusFundPrintData(
                detail.guideVersionNo(),
                detail.guideName(),
                formatSavedAt(detail.completedAt()),
                investmentStyleLabel(planResult.investmentStyle()),

                formatWon(calculation.selectedAccountBalance()),
                formatWon(calculation.adjustedLivingExpense()),
                formatWon(calculation.adjustedScheduledExpense()),
                formatWon(calculation.adjustedEmergencyFund()),
                formatWon(calculation.finalSurplusAmount()),

                allocations,
                planResult.reasons(),
                etfs
        );
    }

    private String findAllocationAmount(
            List<GuideVersionDetailResponse.Allocation> allocations,
            String assetType
    ) {
        return allocations.stream()
                .filter(allocation ->
                        assetType.equals(allocation.assetType())
                )
                .findFirst()
                .map(GuideVersionDetailResponse.Allocation::allocationAmount)
                .map(this::formatWon)
                .orElse("-");
    }

    private String investmentStyleLabel(String investmentStyle) {
        if (investmentStyle == null) {
            return "-";
        }

        return switch (investmentStyle) {
            case "STABLE" -> "안정형";
            case "BALANCED" -> "균형형";
            case "AGGRESSIVE" -> "공격형";
            default -> investmentStyle;
        };
    }

    private String assetTypeLabel(String assetType) {
        if (assetType == null) {
            return "-";
        }

        return switch (assetType) {
            case "CASH" -> "현금";
            case "ETF" -> "ETF";
            case "FUND" -> "펀드";
            default -> assetType;
        };
    }

    private int assetOrder(String assetType) {
        if (assetType == null) {
            return 99;
        }

        return switch (assetType) {
            case "CASH" -> 1;
            case "ETF" -> 2;
            case "FUND" -> 3;
            default -> 99;
        };
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }

        return String.format(
                Locale.KOREA,
                "%,d원",
                amount.longValue()
        );
    }

    private String formatRatio(BigDecimal ratio) {
        if (ratio == null) {
            return "-";
        }

        return ratio.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }

        String value = rate.stripTrailingZeros().toPlainString();

        if (rate.signum() > 0) {
            value = "+" + value;
        }

        return value + "%";
    }

    private String formatSavedAt(LocalDateTime savedAt) {
        if (savedAt == null) {
            return "-";
        }

        return savedAt.format(SAVED_AT_FORMATTER);
    }

}
