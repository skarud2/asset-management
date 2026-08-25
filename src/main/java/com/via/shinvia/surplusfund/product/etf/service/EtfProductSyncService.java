package com.via.shinvia.surplusfund.product.etf.service;

import com.via.shinvia.surplusfund.product.etf.client.EtfExternalApiClient;
import com.via.shinvia.surplusfund.product.etf.dto.EtfExternalResponse;
import com.via.shinvia.surplusfund.product.etf.dto.EtfSyncResponse;
import com.via.shinvia.surplusfund.product.etf.mapper.EtfProductMapper;
import com.via.shinvia.surplusfund.product.etf.model.EtfProduct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EtfProductSyncService {

    public static final String SOURCE_TYPE = "FSC_SECURITIES_PRICE_API";

    private static final int PAGE_SIZE = 1000;
    private static final int LATEST_DATE_LOOKBACK_DAYS = 7;
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final EtfExternalApiClient etfExternalApiClient;
    private final EtfProductMapper etfProductMapper;
    private final TransactionTemplate transactionTemplate;

    public EtfProductSyncService(
            EtfExternalApiClient etfExternalApiClient,
            EtfProductMapper etfProductMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.etfExternalApiClient = etfExternalApiClient;
        this.etfProductMapper = etfProductMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public EtfSyncResponse sync(LocalDate requestedBaseDate) {
        FetchResult fetched = requestedBaseDate == null
                ? fetchLatestAvailableDate()
                : fetchExactDate(requestedBaseDate);

        List<EtfProduct> products = fetched.items().stream()
                .map(this::toProduct)
                .filter(this::hasRequiredFields)
                .toList();

        int upsertedCount = transactionTemplate.execute(status -> {
            int count = 0;
            for (EtfProduct product : products) {
                int catalogCount = etfProductMapper.upsertCatalog(product);
                if (catalogCount < 1 || product.getInvestmentProductId() == null) {
                    throw new IllegalStateException(
                            "ETF 공통 카탈로그 저장에 실패했습니다: " + product.getProductCode()
                    );
                }

                int detailCount = etfProductMapper.upsertDetail(product);
                if (detailCount < 1) {
                    throw new IllegalStateException(
                            "ETF 상세 시세 저장에 실패했습니다: " + product.getProductCode()
                    );
                }
                count++;
            }
            return count;
        });

        return new EtfSyncResponse(
                fetched.baseDate(),
                fetched.items().size(),
                upsertedCount == 0 ? 0 : upsertedCount,
                SOURCE_TYPE
        );
    }

    private FetchResult fetchLatestAvailableDate() {
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset <= LATEST_DATE_LOOKBACK_DAYS; offset++) {
            LocalDate candidate = today.minusDays(offset);
            EtfExternalApiClient.EtfPage firstPage =
                    etfExternalApiClient.fetchPage(candidate, 1, PAGE_SIZE);
            if (!firstPage.items().isEmpty()) {
                return fetchRemainingPages(candidate, firstPage);
            }
        }
        throw new IllegalStateException("최근 7일 이내 조회 가능한 ETF 시세가 없습니다.");
    }

    private FetchResult fetchExactDate(LocalDate baseDate) {
        EtfExternalApiClient.EtfPage firstPage =
                etfExternalApiClient.fetchPage(baseDate, 1, PAGE_SIZE);
        if (firstPage.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "해당 기준일의 ETF 시세가 없습니다: " + baseDate
            );
        }
        return fetchRemainingPages(baseDate, firstPage);
    }

    private FetchResult fetchRemainingPages(
            LocalDate baseDate,
            EtfExternalApiClient.EtfPage firstPage
    ) {
        List<EtfExternalResponse.Item> allItems = new ArrayList<>(firstPage.items());
        int totalPages = Math.max(
                1,
                (int) Math.ceil((double) firstPage.totalCount() / firstPage.numOfRows())
        );

        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            allItems.addAll(
                    etfExternalApiClient.fetchPage(baseDate, pageNo, PAGE_SIZE).items()
            );
        }
        return new FetchResult(baseDate, List.copyOf(allItems));
    }

    private EtfProduct toProduct(EtfExternalResponse.Item item) {
        EtfProduct product = new EtfProduct();
        product.setProductType("ETF");
        product.setProductCode(trimToNull(item.srtnCd()));
        product.setIsinCode(trimToNull(item.isinCd()));
        product.setProductName(trimToNull(item.itmsNm()));
        product.setSourceType(SOURCE_TYPE);
        product.setActive(true);
        product.setLastSyncedAt(LocalDateTime.now());

        product.setPriceBaseDate(parseDate(item.basDt()));
        product.setClosingPrice(item.clpr());
        product.setPreviousDayChange(item.vs());
        product.setFluctuationRate(item.fltRt());
        product.setNav(item.nav());
        product.setOpeningPrice(item.mkp());
        product.setHighPrice(item.hipr());
        product.setLowPrice(item.lopr());
        product.setTradingVolume(toLong(item.trqu()));
        product.setTradingValue(item.trPrc());
        product.setListedShareCount(toLong(item.stLstgCnt()));
        product.setMarketCap(item.mrktTotAmt());
        product.setNetAssetTotalAmount(item.nPptTotAmt());
        product.setBaseIndexName(trimToNull(item.bssIdxIdxNm()));
        product.setBaseIndexClose(item.bssIdxClpr());
        return product;
    }

    private boolean hasRequiredFields(EtfProduct product) {
        return product.getProductCode() != null
                && product.getProductName() != null
                && product.getPriceBaseDate() != null;
    }

    private static LocalDate parseDate(String value) {
        String normalized = normalizeNumber(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized, BASIC_DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("ETF 기준일자 형식이 올바르지 않습니다: " + value);
        }
    }

    private static Long toLong(BigDecimal value) {
        return value == null ? null : value.longValueExact();
    }

    private static String normalizeNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace(",", "");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record FetchResult(
            LocalDate baseDate,
            List<EtfExternalResponse.Item> items
    ) {
    }
}
