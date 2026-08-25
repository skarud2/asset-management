package com.via.shinvia.report.dto.request;

import com.via.shinvia.report.dto.ReportCardSelection;

import java.util.List;

public record ReportLayoutSaveRequest(List<ReportCardSelection> cards) {
}
