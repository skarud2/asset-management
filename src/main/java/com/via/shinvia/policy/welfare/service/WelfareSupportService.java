package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.welfare.dto.WelfareSupportSearchDTO;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 복합지원상품 DB 조회 기능
public class WelfareSupportService {
    private final WelfareSupportProductRepository repository;

    public FinancialProductPageDTO findAll(WelfareSupportSearchDTO search) {
        long total = repository.count(search);
        var products = repository.search(search, (long) search.getPage() * search.getSize(), search.getSize())
                .stream().map(this::toListItem).toList();
        return page(products, search.getPage(), search.getSize(), total);
    }

    public FinancialProductDetailDTO findById(String id) {
        WelfareSupportProduct product = repository.findByExternalId(id)
                .orElseThrow(() -> new IllegalArgumentException("복합지원상품을 찾을 수 없습니다."));
        return FinancialProductDetailDTO.builder()
                .title(product.getProductName()).badge("복합지원").listPath("/welfare-support")
                .summary(values(
                        "복지 유형", product.getWelfareType(),
                        "운영 기관", product.getInstitutionName(),
                        "담당 기관", product.getResponsibleInstitution()))
                .conditions(values(
                        "지원 대상", product.getSupportTarget(),
                        "연령 조건", product.getAgeCondition(),
                        "지원 내용", product.getSupportContent()))
                .application(values("신청 방법", product.getApplicationMethod()))
                .relatedSite(product.getRelatedUrl()).build();
    }

    private FinancialProductDTO toListItem(WelfareSupportProduct product) {
        return FinancialProductDTO.builder()
                .id(product.getExternalId()).title(product.getProductName()).badge("복합지원")
                .firstLabel("지원 대상").firstValue(value(product.getSupportTarget()))
                .secondLabel("대상 연령").secondValue(value(product.getAgeCondition()))
                .institution(value(product.getInstitutionName()))
                .searchText(String.join(" ", value(product.getWelfareType()),
                        value(product.getSupportContent()), value(product.getResponsibleInstitution()))).build();
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
