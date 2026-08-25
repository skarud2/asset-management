package com.via.shinvia.report.dto.response;

import com.via.shinvia.report.dto.ReportCardSelection;

import java.util.List;

public record ReportLayoutResponse(List<ReportCardSelection> cards) {
}
