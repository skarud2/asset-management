package com.via.shinvia.surplusfund.product.fund.service;

import com.via.shinvia.surplusfund.product.fund.dto.FundCsvRow;
import com.via.shinvia.surplusfund.product.fund.dto.FundImportResponse;
import com.via.shinvia.surplusfund.product.fund.importer.FundCsvReader;
import com.via.shinvia.surplusfund.product.fund.mapper.FundProductMapper;
import com.via.shinvia.surplusfund.product.fund.model.FundProduct;
import com.via.shinvia.surplusfund.product.fund.util.FundProductCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FundProductImportService {
    private static final String CSV_PATH = "data/fund/fund_20241231.csv";

    private static final LocalDate DISCLOSURE_BASE_DATE = LocalDate.of(2024, 12, 31);

    private static final Charset CSV_CHARSET = StandardCharsets.UTF_8;

    private static final String PRODUCT_TYPE = "FUND";
    private static final String SOURCE_TYPE = "IBK_FUND_CSV";

    private final FundCsvReader fundCsvReader;
    private final FundProductMapper fundProductMapper;

    @Transactional(rollbackFor = Exception.class)
    public FundImportResponse importDefaultCsv() {

        List<FundCsvRow> rows = readRows();

        Map<String, FundCsvRow> uniqueRows = validateAndRemoveDuplicates(rows);

        LocalDateTime syncedAt = LocalDateTime.now();

        for (Map.Entry<String, FundCsvRow> entry : uniqueRows.entrySet()) {

            String productCode = entry.getKey();
            FundCsvRow row = entry.getValue();

            FundProduct product = toFundProduct(productCode, row, syncedAt);

            fundProductMapper.upsertCatalog(product);

            if (product.getInvestmentProductId() == null) {
                throw new IllegalStateException("펀드 카탈로그 저장 후 상품 ID를 확인할 수 없습니다. productCode=" + productCode);
            }

            fundProductMapper.upsertDetail(product);
        }

        int savedDetailCount = fundProductMapper.countFundDetailsByBaseDate(DISCLOSURE_BASE_DATE);

        return new FundImportResponse(
                SOURCE_TYPE,
                DISCLOSURE_BASE_DATE,
                rows.size(),
                uniqueRows.size(),
                savedDetailCount,
                "중소기업은행 펀드 CSV 적재 완료"
        );
    }

    private List<FundCsvRow> readRows() {

        ClassPathResource resource = new ClassPathResource(CSV_PATH);

        if (!resource.exists()) {
            throw new IllegalStateException("펀드 CSV 파일이 없습니다: " + CSV_PATH);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return fundCsvReader.read(inputStream, CSV_CHARSET);
        } catch (IOException e) {
            throw new IllegalStateException("펀드 CSV 파일을 열 수 없습니다: " + CSV_PATH, e);
        }
    }

    private Map<String, FundCsvRow> validateAndRemoveDuplicates(
            List<FundCsvRow> rows
    ) {

        Map<String, FundCsvRow> uniqueRows =
                new LinkedHashMap<>();

        for (FundCsvRow row : rows) {

            String productCode =
                    FundProductCodeGenerator.generate(
                            row.providerName(),
                            row.productName(),
                            row.fundType(),
                            row.fundGrade(),
                            row.upfrontFeeRate(),
                            row.totalExpenseRate()
                    );

            FundCsvRow previous = uniqueRows.putIfAbsent(productCode, row);

            if (previous != null && !previous.equals(row)) {
                throw new IllegalStateException(
                        "동일한 펀드 상품 식별정보에 서로 다른 데이터가 존재합니다. "
                                + "productCode=" + productCode
                );
            }
        }

        return uniqueRows;
    }

    private FundProduct toFundProduct(
            String productCode,
            FundCsvRow row,
            LocalDateTime syncedAt
    ) {

        FundProduct product = new FundProduct();

        // investment_product_catalog
        product.setProductType(PRODUCT_TYPE);
        product.setProductCode(productCode);
        product.setIsinCode(null);
        product.setProductName(row.productName());
        product.setProviderName(row.providerName());
        product.setCategory(row.fundType());
        product.setSourceType(SOURCE_TYPE);
        product.setActive(true);
        product.setLastSyncedAt(syncedAt);

        // fund_product_detail
        product.setDisclosureBaseDate(DISCLOSURE_BASE_DATE);
        product.setReturn1Month(row.return1Month());
        product.setReturn3Months(row.return3Months());
        product.setReturn6Months(row.return6Months());
        product.setReturn12Months(row.return12Months());
        product.setFundGrade(row.fundGrade());
        product.setUpfrontFeeRate(row.upfrontFeeRate());
        product.setTotalExpenseRate(row.totalExpenseRate());

        return product;
    }
}
