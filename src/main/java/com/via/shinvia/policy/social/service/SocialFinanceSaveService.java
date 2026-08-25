package com.via.shinvia.policy.social.service;

import com.via.shinvia.policy.social.entity.SocialFinanceProduct;
import com.via.shinvia.policy.social.repository.SocialFinanceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
// 사회연대금융상품 일괄 저장 기능
public class SocialFinanceSaveService {
    private final SocialFinanceProductRepository repository;

    @Transactional
    public int saveAll(List<SocialFinanceProduct> products) {
        List<SocialFinanceProduct> validProducts = products.stream()
                .filter(product -> product.getExternalId() != null && product.getProductName() != null)
                .toList();
        if (validProducts.isEmpty()) {
            throw new IllegalStateException("사회연대금융 API 결과가 비어 있어 기존 데이터를 유지합니다.");
        }
        repository.deactivateAll();
        validProducts.forEach(repository::upsert);
        return validProducts.size();
    }
}
