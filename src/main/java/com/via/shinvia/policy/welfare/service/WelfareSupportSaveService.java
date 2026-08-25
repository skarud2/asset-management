package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WelfareSupportSaveService {

    private final WelfareSupportProductRepository repository;

    @Transactional
    public int saveAll(List<WelfareSupportProduct> products) {
        List<WelfareSupportProduct> validProducts = getValidProducts(products);

        repository.deactivateAll();
        validProducts.forEach(repository::upsert);

        return validProducts.size();
    }

    @Transactional
    public int saveAllBySourceType(
            String sourceType,
            List<WelfareSupportProduct> products
    ) {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType은 필수입니다.");
        }

        List<WelfareSupportProduct> validProducts = getValidProducts(products);

        repository.deactivateBySourceType(sourceType);
        validProducts.forEach(repository::upsert);

        return validProducts.size();
    }

    private List<WelfareSupportProduct> getValidProducts(
            List<WelfareSupportProduct> products
    ) {
        List<WelfareSupportProduct> validProducts = products.stream()
                .filter(product -> product.getExternalId() != null)
                .filter(product -> product.getProductName() != null)
                .toList();

        if (validProducts.isEmpty()) {
            throw new IllegalStateException(
                    "복지상품 API 결과가 비어 있어 기존 데이터를 유지합니다."
            );
        }

        Map<String, WelfareSupportProduct> deduplicated = new LinkedHashMap<>();
        for (WelfareSupportProduct product : validProducts) {
            deduplicated.put(product.getExternalId(), product);
        }

        return List.copyOf(deduplicated.values());
    }
}
