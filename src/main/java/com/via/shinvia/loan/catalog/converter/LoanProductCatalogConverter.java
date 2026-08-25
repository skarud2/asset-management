package com.via.shinvia.loan.catalog.converter;

import com.via.shinvia.loan.catalog.dto.command.LoanProductCatalogModels;
import com.via.shinvia.loan.catalog.dto.external.credit.CreditResponse;
import com.via.shinvia.loan.catalog.dto.external.jeonse.JeonseResponse;
import com.via.shinvia.loan.catalog.dto.external.mortgage.MortgageResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LoanProductCatalogConverter {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public List<LoanProductCatalogModels.HousingProduct> mortgage(
            List<MortgageResponse.Base> bases,
            List<MortgageResponse.Option> options
    ) {
        List<MortgageResponse.Base> safeBases =
                bases == null ? List.of() : bases;
        List<MortgageResponse.Option> safeOptions =
                options == null ? List.of() : options;

        Map<String, List<MortgageResponse.Option>> grouped =
                safeOptions.stream().collect(Collectors.groupingBy(
                        o -> key(o.disclosureMonth(), o.financeCompanyCode(), o.productCode())
                ));

        return safeBases.stream().map(base -> {
            List<MortgageResponse.Option> matched =
                    grouped.getOrDefault(
                            key(base.disclosureMonth(), base.financeCompanyCode(), base.productCode()),
                            List.of()
                    );

            BigDecimal min = matched.stream()
                    .map(MortgageResponse.Option::minRate)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal max = matched.stream()
                    .map(MortgageResponse.Option::maxRate)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            LoanProductCatalogModels.CatalogProduct catalog = new LoanProductCatalogModels.CatalogProduct(
                    "FINLIFE_MORTGAGE",
                    "FINLIFE_MORTGAGE:" + base.financeCompanyCode() + ":" + base.productCode(),
                    base.financeCompanyCode(),
                    base.productCode(),
                    "",
                    base.disclosureMonth(),
                    base.productName(),
                    "MORTGAGE",
                    min,
                    max,
                    null,
                    null,
                    "주택담보대출",
                    active(parseDate(base.disclosureEndDay())),
                    base.institutionName(),
                    null,
                    base.joinWay(),
                    parseDate(base.disclosureStartDay()),
                    parseDate(base.disclosureEndDay()),
                    parseDateTime(base.submittedDay()),
                    "http://finlife.fss.or.kr/finlifeapi/mortgageLoanProductsSearch.json",
                    LocalDateTime.now()
            );

            LoanProductCatalogModels.HousingDetail detail =
                    new LoanProductCatalogModels.HousingDetail(
                            base.incidentalExpense(),
                            base.earlyRepaymentFee(),
                            base.delinquencyRate(),
                            base.loanLimit()
                    );

            List<LoanProductCatalogModels.HousingOption> converted =
                    matched.stream().map(o ->
                            new LoanProductCatalogModels.HousingOption(
                                    o.collateralTypeCode() == null ? "" : o.collateralTypeCode(),
                                    o.collateralTypeName(),
                                    o.repaymentTypeCode(),
                                    o.repaymentTypeName(),
                                    o.rateTypeCode(),
                                    o.rateTypeName(),
                                    o.minRate(),
                                    o.maxRate(),
                                    o.averageRate()
                            )
                    ).toList();

            return new LoanProductCatalogModels.HousingProduct(catalog, detail, converted);
        }).toList();
    }

    public List<LoanProductCatalogModels.HousingProduct> jeonse(
            List<JeonseResponse.Base> bases,
            List<JeonseResponse.Option> options
    ) {
        List<JeonseResponse.Base> safeBases =
                bases == null ? List.of() : bases;
        List<JeonseResponse.Option> safeOptions =
                options == null ? List.of() : options;

        Map<String, List<JeonseResponse.Option>> grouped =
                safeOptions.stream().collect(Collectors.groupingBy(
                        o -> key(o.disclosureMonth(), o.financeCompanyCode(), o.productCode())
                ));

        return safeBases.stream().map(base -> {
            List<JeonseResponse.Option> matched =
                    grouped.getOrDefault(
                            key(base.disclosureMonth(), base.financeCompanyCode(), base.productCode()),
                            List.of()
                    );

            BigDecimal min = matched.stream()
                    .map(JeonseResponse.Option::minRate)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal max = matched.stream()
                    .map(JeonseResponse.Option::maxRate)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            LoanProductCatalogModels.CatalogProduct catalog = new LoanProductCatalogModels.CatalogProduct(
                    "FINLIFE_JEONSE",
                    "FINLIFE_JEONSE:" + base.financeCompanyCode() + ":" + base.productCode(),
                    base.financeCompanyCode(),
                    base.productCode(),
                    "",
                    base.disclosureMonth(),
                    base.productName(),
                    "JEONSE",
                    min,
                    max,
                    null,
                    null,
                    "전세자금대출",
                    active(parseDate(base.disclosureEndDay())),
                    base.institutionName(),
                    null,
                    base.joinWay(),
                    parseDate(base.disclosureStartDay()),
                    parseDate(base.disclosureEndDay()),
                    parseDateTime(base.submittedDay()),
                    "http://finlife.fss.or.kr/finlifeapi/rentHouseLoanProductsSearch.json",
                    LocalDateTime.now()
            );

            LoanProductCatalogModels.HousingDetail detail =
                    new LoanProductCatalogModels.HousingDetail(
                            base.incidentalExpense(),
                            base.earlyRepaymentFee(),
                            base.delinquencyRate(),
                            base.loanLimit()
                    );

            List<LoanProductCatalogModels.HousingOption> converted =
                    matched.stream().map(o ->
                            new LoanProductCatalogModels.HousingOption(
                                    "",
                                    null,
                                    o.repaymentTypeCode(),
                                    o.repaymentTypeName(),
                                    o.rateTypeCode(),
                                    o.rateTypeName(),
                                    o.minRate(),
                                    o.maxRate(),
                                    o.averageRate()
                            )
                    ).toList();

            return new LoanProductCatalogModels.HousingProduct(catalog, detail, converted);
        }).toList();
    }

    public List<LoanProductCatalogModels.CreditProduct> credit(
            List<CreditResponse.Base> bases,
            List<CreditResponse.Option> options
    ) {
        List<CreditResponse.Base> safeBases =
                bases == null ? List.of() : bases;
        List<CreditResponse.Option> safeOptions =
                options == null ? List.of() : options;

        Map<String, List<CreditResponse.Option>> grouped =
                safeOptions.stream().collect(Collectors.groupingBy(
                        o -> key(
                                o.disclosureMonth(),
                                o.financeCompanyCode(),
                                o.productCode(),
                                o.productTypeCode()
                        )
                ));

        return safeBases.stream()
                .filter(base -> !"3".equals(base.productTypeCode()))
                .map(base -> {
                    List<CreditResponse.Option> matched =
                            grouped.getOrDefault(
                                    key(
                                            base.disclosureMonth(),
                                            base.financeCompanyCode(),
                                            base.productCode(),
                                            base.productTypeCode()
                                    ),
                                    List.of()
                            );

                    List<BigDecimal> actualRates = matched.stream()
                            .filter(o -> "A".equals(o.categoryCode()))
                            .flatMap(o -> o.scoreRates().stream())
                            .toList();

                    BigDecimal min = actualRates.stream()
                            .min(BigDecimal::compareTo)
                            .orElse(null);

                    BigDecimal max = actualRates.stream()
                            .max(BigDecimal::compareTo)
                            .orElse(null);

                    LoanProductCatalogModels.CatalogProduct catalog = new LoanProductCatalogModels.CatalogProduct(
                            "FINLIFE_CREDIT",
                            "FINLIFE_CREDIT:" + base.financeCompanyCode()
                                    + ":" + base.productCode()
                                    + ":" + base.productTypeCode(),
                            base.financeCompanyCode(),
                            base.productCode(),
                            base.productTypeCode(),
                            base.disclosureMonth(),
                            base.productName(),
                            "CREDIT",
                            min,
                            max,
                            null,
                            null,
                            base.productTypeName(),
                            active(parseDate(base.disclosureEndDay())),
                            base.institutionName(),
                            null,
                            base.joinWay(),
                            parseDate(base.disclosureStartDay()),
                            parseDate(base.disclosureEndDay()),
                            parseDateTime(base.submittedDay()),
                            "http://finlife.fss.or.kr/finlifeapi/creditLoanProductsSearch.json",
                            LocalDateTime.now()
                    );

                    LoanProductCatalogModels.CreditDetail detail =
                            new LoanProductCatalogModels.CreditDetail(
                                    base.creditBureauName(),
                                    base.productTypeCode(),
                                    base.productTypeName()
                            );

                    List<LoanProductCatalogModels.CreditOption> converted =
                            matched.stream().map(o ->
                                    new LoanProductCatalogModels.CreditOption(
                                            o.categoryCode(),
                                            o.categoryName(),
                                            o.over900(),
                                            o.from801To900(),
                                            o.from701To800(),
                                            o.from601To700(),
                                            o.from501To600(),
                                            o.from401To500(),
                                            o.from301To400(),
                                            o.belowOrEqual300(),
                                            o.averageRate()
                                    )
                            ).toList();

                    return new LoanProductCatalogModels.CreditProduct(catalog, detail, converted);
                })
                .toList();
    }

    private String key(String... values) {
        return String.join("|", values);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank()
                ? null
                : LocalDate.parse(value.trim(), DATE);
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank()
                ? null
                : LocalDateTime.parse(value.trim(), DATE_TIME);
    }

    private boolean active(LocalDate endDate) {
        return endDate == null || !endDate.isBefore(LocalDate.now());
    }
}
