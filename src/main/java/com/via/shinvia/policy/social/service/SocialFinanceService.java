package com.via.shinvia.policy.social.service;

import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.social.dto.SocialFinanceSearchDTO;
import com.via.shinvia.policy.social.entity.SocialFinanceProduct;
import com.via.shinvia.policy.social.repository.SocialFinanceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 사회연대금융상품 DB 조회 기능
public class SocialFinanceService {
    private final SocialFinanceProductRepository repository;

    public FinancialProductPageDTO findAll(SocialFinanceSearchDTO search) {
        long total = repository.count(search);
        var products = repository.search(search, (long) search.getPage() * search.getSize(), search.getSize())
                .stream().map(this::toListItem).toList();
        return page(products, search.getPage(), search.getSize(), total);
    }

    public FinancialProductDetailDTO findById(String id) {
        SocialFinanceProduct product = repository.findByExternalId(id)
                .orElseThrow(() -> new IllegalArgumentException("사회연대금융상품을 찾을 수 없습니다."));
        return FinancialProductDetailDTO.builder()
                .title(product.getProductName()).badge("사회연대금융").listPath("/social-finance")
                .summary(values(
                        "상품 구분", product.getProductCategory(),
                        "지원 금액", product.getSupportAmount(),
                        "취급 기관", product.getHandlingInstitution(),
                        "운영 기관", product.getInstitutionName()))
                .conditions(values(
                        "지원 대상", product.getSupportTarget(),
                        "사업 유형", product.getBusinessType(),
                        "지원 방식", product.getSupportMethod()))
                .application(values("신청 방법", product.getApplicationMethod()))
                .relatedSite(product.getRelatedUrl()).build();
    }

    private FinancialProductDTO toListItem(SocialFinanceProduct product) {
        return FinancialProductDTO.builder()
                .id(product.getExternalId()).title(product.getProductName()).badge("사회연대금융")
                .firstLabel("지원 대상").firstValue(value(product.getSupportTarget()))
                .secondLabel("상품 구분").secondValue(value(product.getProductCategory()))
                .institution(value(product.getInstitutionName()))
                .searchText(String.join(" ", value(product.getBusinessType()),
                        value(product.getSupportMethod()), value(product.getHandlingInstitution()))).build();
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
