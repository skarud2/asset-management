package com.via.shinvia.loan.catalog.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LoanProductCatalogModels {

    private LoanProductCatalogModels() {
    }

    public record CatalogProduct(
            String sourceType,
            String sourceProductKey,
            String sourceFinanceCode,
            String sourceProductCode,
            String sourceProductSubtype,
            String disclosureMonth,
            String productName,
            String loanType,
            BigDecimal minRate,
            BigDecimal maxRate,
            BigDecimal maxLimitAmount,
            Integer maxPeriodMonths,
            String targetDescription,
            Boolean active,
            String institutionName,
            Long institutionId,
            String joinWay,
            LocalDate disclosureStartDate,
            LocalDate disclosureEndDate,
            LocalDateTime sourceSubmittedAt,
            String sourceUrl,
            LocalDateTime collectedAt
    ) {
    }

    public record HousingDetail(
            String incidentalExpense,
            String earlyRepaymentFee,
            String delinquencyRate,
            String loanLimit
    ) {
    }

    public record HousingOption(
            String collateralTypeCode,
            String collateralTypeName,
            String repaymentTypeCode,
            String repaymentTypeName,
            String rateTypeCode,
            String rateTypeName,
            BigDecimal minRate,
            BigDecimal maxRate,
            BigDecimal averageRate
    ) {
    }

    public record HousingProduct(
            CatalogProduct catalog,
            HousingDetail detail,
            List<HousingOption> options
    ) {
    }

    public record CreditDetail(
            String creditBureauName,
            String productTypeCode,
            String productTypeName
    ) {
    }

    public record CreditOption(
            String categoryCode,
            String categoryName,
            BigDecimal over900,
            BigDecimal from801To900,
            BigDecimal from701To800,
            BigDecimal from601To700,
            BigDecimal from501To600,
            BigDecimal from401To500,
            BigDecimal from301To400,
            BigDecimal belowOrEqual300,
            BigDecimal averageRate
    ) {
    }

    public record CreditProduct(
            CatalogProduct catalog,
            CreditDetail detail,
            List<CreditOption> options
    ) {
    }
}
