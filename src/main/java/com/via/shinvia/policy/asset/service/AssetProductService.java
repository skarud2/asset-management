package com.via.shinvia.policy.asset.service;

import com.via.shinvia.policy.asset.dto.AssetProductSearchDTO;
import com.via.shinvia.policy.asset.entity.AssetFormationProduct;
import com.via.shinvia.policy.asset.repository.AssetFormationProductRepository;
import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 자산형성상품 DB 조회 기능
public class AssetProductService {
    private final AssetFormationProductRepository repository;

    public FinancialProductPageDTO findAll(AssetProductSearchDTO search) {
        long total = repository.count(search);
        var products = repository.search(search, (long) search.getPage() * search.getSize(), search.getSize())
                .stream().map(this::toListItem).toList();
        return page(products, search.getPage(), search.getSize(), total);
    }

    public FinancialProductDetailDTO findById(String id) {
        AssetFormationProduct product = repository.findByExternalId(id)
                .orElseThrow(() -> new IllegalArgumentException("자산형성상품을 찾을 수 없습니다."));
        Map<String, String> summary = values(
                "가입 기간", product.getSubscriptionPeriod(),
                "적립 방식", product.getSavingMethod(),
                "정부 지원", product.getGovernmentSupport(),
                "만기 혜택", product.getMaturityBenefit(),
                "운영 기관", product.getInstitutionName());
        Map<String, String> conditions = values(
                "가입 대상", product.getSubscriptionTarget(),
                "소득 조건", product.getIncomeCondition(),
                "연령 조건", product.getAgeCondition(),
                "지원 지역", product.getSupportRegion());
        return FinancialProductDetailDTO.builder()
                .title(product.getProductName()).badge("자산형성상품").listPath("/asset-products")
                .summary(summary).conditions(conditions)
                .application(values("신청 방법", product.getApplicationMethod()))
                .relatedSite(product.getRelatedUrl()).build();
    }

    private FinancialProductDTO toListItem(AssetFormationProduct product) {
        return FinancialProductDTO.builder()
                .id(product.getExternalId()).title(product.getProductName()).badge("자산형성")
                .firstLabel("정부 지원").firstValue(value(product.getGovernmentSupport()))
                .secondLabel("가입 기간").secondValue(value(product.getSubscriptionPeriod()))
                .institution(value(product.getInstitutionName()))
                .searchText(String.join(" ", value(product.getSubscriptionTarget()),
                        value(product.getIncomeCondition()), value(product.getAgeCondition()),
                        value(product.getSupportRegion()))).build();
    }

    private FinancialProductPageDTO page(java.util.List<FinancialProductDTO> products, int page, int size, long total) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return FinancialProductPageDTO.builder().products(products).page(page).size(size)
                .totalElements(total).totalPages(pages).first(page == 0)
                .last(pages == 0 || page >= pages - 1).build();
    }

    private Map<String, String> values(String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) if (pairs[i + 1] != null && !pairs[i + 1].isBlank()) result.put(pairs[i], pairs[i + 1]);
        return result;
    }

    private String value(String value) { return value == null || value.isBlank() ? "-" : value; }
}
