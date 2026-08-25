package com.via.shinvia.surplusfund.product.etf.service;

import com.via.shinvia.surplusfund.product.etf.dto.EtfProductListResponse;
import com.via.shinvia.surplusfund.product.etf.dto.EtfProductResponse;
import com.via.shinvia.surplusfund.product.etf.mapper.EtfProductMapper;
import com.via.shinvia.surplusfund.product.etf.model.EtfProduct;
import com.via.shinvia.surplusfund.product.etf.model.EtfProductSort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EtfProductService {

    public static final String PRODUCT_NOTICE =
            "표시된 정보는 공식 시세 기준의 탐색·비교 정보이며, 특정 상품의 투자권유·자문이나 수익 보장을 의미하지 않습니다.";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final EtfProductMapper etfProductMapper;

    public EtfProductService(EtfProductMapper etfProductMapper) {
        this.etfProductMapper = etfProductMapper;
    }

    public EtfProductListResponse findProducts(
            String keyword,
            EtfProductSort sort,
            Integer limit
    ) {
        int normalizedLimit = normalizeLimit(limit);
        String normalizedKeyword = normalizeKeyword(keyword);
        EtfProductSort normalizedSort = sort == null
                ? EtfProductSort.TRADING_VALUE_DESC
                : sort;

        List<EtfProductResponse> products = etfProductMapper
                .findProducts(normalizedKeyword, normalizedSort, normalizedLimit)
                .stream()
                .map(EtfProductResponse::from)
                .toList();

        return new EtfProductListResponse(
                etfProductMapper.findLatestBaseDate(),
                products.size(),
                products,
                PRODUCT_NOTICE
        );
    }

    public EtfProductResponse findById(Long investmentProductId) {
        if (investmentProductId == null || investmentProductId <= 0) {
            throw new IllegalArgumentException("ETF 상품 ID가 올바르지 않습니다.");
        }

        EtfProduct product = etfProductMapper.findById(investmentProductId);
        if (product == null) {
            throw new NoSuchElementException("ETF 상품을 찾을 수 없습니다.");
        }
        return EtfProductResponse.from(product);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit은 1 이상 100 이하여야 합니다.");
        }
        return limit;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이하여야 합니다.");
        }
        return trimmed;
    }
}

