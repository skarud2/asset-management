package com.via.shinvia.policy.asset.service;

import com.via.shinvia.policy.asset.entity.AssetFormationProduct;
import com.via.shinvia.policy.asset.repository.AssetFormationProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
// 자산형성상품 일괄 저장 기능
public class AssetProductSaveService {
    private final AssetFormationProductRepository repository;

    @Transactional
    public int saveAll(List<AssetFormationProduct> products) {
        List<AssetFormationProduct> validProducts = products.stream()
                .filter(product -> product.getExternalId() != null && product.getProductName() != null)
                .toList();
        if (validProducts.isEmpty()) {
            throw new IllegalStateException("자산형성상품 API 결과가 비어 있어 기존 데이터를 유지합니다.");
        }
        repository.deactivateAll();
        validProducts.forEach(repository::upsert);
        return validProducts.size();
    }
}
