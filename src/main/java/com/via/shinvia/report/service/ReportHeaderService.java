package com.via.shinvia.report.service;

import com.via.shinvia.report.dto.response.ReportHeaderResponse;
import com.via.shinvia.report.overview.ReportOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportHeaderService {

    private final FinancialScoreCalculator financialScoreCalculator;
    private final ReportOverviewService reportOverviewService;

    public ReportHeaderResponse getHeader(Long userId) {
        FinancialScoreCalculator.Result result = financialScoreCalculator.calculate(userId);

        return new ReportHeaderResponse(
                result.totalScore(),
                gradeFor(result.totalScore()),
                result.dimensions().stream()
                        .map(d -> new ReportHeaderResponse.Dimension(d.key(), d.label(), d.score(), d.weightPercent()))
                        .toList(),
                reportOverviewService.getOverview(userId)
        );
    }

    private String gradeFor(BigDecimal totalScore) {
        double score = totalScore.doubleValue();
        if (score >= 80) return "우수";
        if (score >= 60) return "양호";
        if (score >= 40) return "보통";
        if (score >= 20) return "주의";
        return "위험";
    }
}
