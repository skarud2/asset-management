package com.via.shinvia.policy.asset.service;

import com.via.shinvia.policy.asset.client.AssetProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 자산형성상품 API 동기화 기능
public class AssetProductSyncService {
    private final AssetProductClient client;
    private final AssetProductSaveService saveService;

    public int synchronize() {
        return saveService.saveAll(client.fetchAll());
    }
}
