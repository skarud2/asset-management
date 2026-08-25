package com.via.shinvia.report.surplusfund.dto;

import java.util.List;

public record SurplusFundPrintData(
        int guideVersionNo,
        String guideName,
        String savedAt,
        String investmentStyle,

        String selectedAccountBalance,
        String livingExpense,
        String scheduledExpense,
        String emergencyFund,
        String finalSurplusAmount,

        List<Allocation> allocations,
        List<String> reasons,
        List<Etf> interestedEtfs) {

    public record Allocation(
            String assetType,
            String label,
            String ratio,
            String amount
    ) { }

    public record Etf(
            int selectionOrder,
            String productName,
            String productCode,
            String priceBaseDate,
            String closingPrice,
            String fluctuationRate
    ) { }
}
