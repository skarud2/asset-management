package com.via.shinvia.surplusfund.guideversion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GuideVersionDetailResponse(
        Long surplusFundGuideVersionId,
        int guideVersionNo,
        String guideName,
        String snapshotSchemaVersion,
        LocalDateTime completedAt,
        Calculation calculation,
        PlanResult planResult,
        List<EtfSnapshot> interestedEtfs
) {
    public GuideVersionDetailResponse {
        interestedEtfs = List.copyOf(interestedEtfs);
    }

    public record Calculation(
            Long surplusFundCalculationId,
            BigDecimal totalCurrentBalance,
            BigDecimal selectedAccountBalance,
            LocalDate estimatedNextIncomeDate,
            LocalDate adjustedNextIncomeDate,
            BigDecimal estimatedLivingExpense,
            BigDecimal adjustedLivingExpense,
            BigDecimal estimatedScheduledExpense,
            BigDecimal adjustedScheduledExpense,
            BigDecimal recommendedEmergencyFund,
            BigDecimal adjustedEmergencyFund,
            BigDecimal calculatedSurplusAmount,
            BigDecimal finalSurplusAmount
    ) {
    }

    public record PlanResult(
            Long surplusFundPlanId,
            BigDecimal operationAmount,
            String investmentStyle,
            String ruleVersion,
            List<String> reasons,
            List<Allocation> allocations
    ) {
        public PlanResult {
            reasons = List.copyOf(reasons);
            allocations = List.copyOf(allocations);
        }
    }

    public record Allocation(
            String assetType,
            BigDecimal allocationRatio,
            BigDecimal allocationAmount
    ) {
    }

    public record EtfSnapshot(
            int selectionOrder,
            Long sourceInvestmentProductId,
            String productCode,
            String isinCode,
            String productName,
            String providerName,
            String category,
            LocalDate priceBaseDate,
            BigDecimal closingPrice,
            BigDecimal previousDayChange,
            BigDecimal fluctuationRate,
            BigDecimal nav,
            BigDecimal openingPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            Long tradingVolume,
            BigDecimal tradingValue,
            Long listedShareCount,
            BigDecimal marketCap,
            BigDecimal netAssetTotalAmount,
            String baseIndexName,
            BigDecimal baseIndexClose,
            LocalDateTime productLastSyncedAt,
            LocalDateTime capturedAt
    ) {
    }
}
