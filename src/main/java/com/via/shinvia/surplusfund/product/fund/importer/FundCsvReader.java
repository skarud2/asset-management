package com.via.shinvia.surplusfund.product.fund.importer;

import com.via.shinvia.surplusfund.product.fund.dto.FundCsvRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Component
public class FundCsvReader {

    private static final String PROVIDER_NAME = "운용사명";
    private static final String PRODUCT_NAME = "상품명";
    private static final String RETURN_1_MONTH = "1개월누적수익률(퍼센트)";
    private static final String RETURN_3_MONTHS = "3개월누적수익률(퍼센트)";
    private static final String RETURN_6_MONTHS = "6개월누적수익률(퍼센트)";
    private static final String RETURN_12_MONTHS = "12개월누적수익률(퍼센트)";
    private static final String FUND_GRADE = "펀드등급";
    private static final String FUND_TYPE = "펀드유형";
    private static final String UPFRONT_FEE_RATE = "선취수수료(퍼센트)";
    private static final String TOTAL_EXPENSE_RATE = "총보수(퍼센트)";

    public List<FundCsvRow> read(InputStream inputStream, Charset charset) {

        try (
                Reader reader = new BufferedReader(
                        new InputStreamReader(inputStream, charset)
                );

                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {

            validateHeaders(parser);

            List<FundCsvRow> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                try {
                    rows.add(toRow(record));
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            "CSV " + record.getRecordNumber()
                                    + "번째 행 변환 실패: "
                                    + e.getMessage(),
                            e
                    );
                }
            }

            return rows;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "펀드 CSV 파일을 읽을 수 없습니다.",
                    e
            );
        }
    }

    private void validateHeaders(CSVParser parser) {

        List<String> requiredHeaders = List.of(
                PROVIDER_NAME,
                PRODUCT_NAME,
                RETURN_1_MONTH,
                RETURN_3_MONTHS,
                RETURN_6_MONTHS,
                RETURN_12_MONTHS,
                FUND_GRADE,
                FUND_TYPE,
                UPFRONT_FEE_RATE,
                TOTAL_EXPENSE_RATE
        );

        for (String header : requiredHeaders) {

            if (!parser.getHeaderMap().containsKey(header)) {
                throw new IllegalArgumentException(
                        "CSV 필수 컬럼이 없습니다: "
                                + header
                                + ", 실제 컬럼="
                                + parser.getHeaderMap().keySet()
                );
            }
        }
    }

    private FundCsvRow toRow(CSVRecord record) {

        String providerName = requiredText(record, PROVIDER_NAME);
        String productName = requiredText(record, PRODUCT_NAME);
        String fundType = requiredText(record, FUND_TYPE);

        return new FundCsvRow(
                providerName,
                productName,
                decimalOrNull(record.get(RETURN_1_MONTH)),
                decimalOrNull(record.get(RETURN_3_MONTHS)),
                decimalOrNull(record.get(RETURN_6_MONTHS)),
                decimalOrNull(record.get(RETURN_12_MONTHS)),
                integerOrNull(record.get(FUND_GRADE)),
                fundType,
                decimalOrNull(record.get(UPFRONT_FEE_RATE)),
                decimalOrNull(record.get(TOTAL_EXPENSE_RATE))
        );
    }

    private String requiredText(CSVRecord record, String header) {

        String value = record.get(header);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    header + " 값이 비어 있습니다."
            );
        }

        return value.trim();
    }

    private BigDecimal decimalOrNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace(",", "")
                .replace("%", "");

        return new BigDecimal(normalized);
    }

    private Integer integerOrNull(String value) {

        BigDecimal decimal = decimalOrNull(value);

        if (decimal == null) {
            return null;
        }

        return decimal.intValueExact();
    }
}
