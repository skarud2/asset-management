package com.via.shinvia.report.service.provider;

import com.via.shinvia.report.futuresim.dto.FuturesimPlanPrintData;
import com.via.shinvia.report.surplusfund.dto.SurplusFundPrintData;

import java.util.List;

public interface ReportCardDataProvider {

    String getCardKey();

    boolean isAvailable();

    CardData getCardData(Long userId, Long refId);

    record CardData(
            String cardKey,
            String title,
            String headlineLabel,
            String headlineValue,
            List<DetailRow> detailRows,
            String note,
            FuturesimPlanPrintData futuresimPrintData,
            SurplusFundPrintData surplusFundPrintData
    ) {
        public record DetailRow(String label, String value) {
        }
    }
}
