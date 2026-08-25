package com.via.shinvia.report.overview.dto;

public record ReportOverviewData(
        FinancialProfileSummary financialProfile,
        MyDataSummary myData
) {
    public record FinancialProfileSummary(
            boolean available,
            String annualIncome,
            String incomeType,
            String employmentStatus,
            String creditScore,
            String liquidAssetAmount
    ) {
    }

    public record MyDataSummary(
            boolean connected,
            int accountCount,
            String totalAccountBalance,
            int cardCount,
            int loanCount,
            String totalLoanBalance
    ) {
    }
}
