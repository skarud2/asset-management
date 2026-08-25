package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.welfare.client.WelfareSupportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 복합지원상품 API 동기화 기능
public class WelfareSupportSyncService {
    private final WelfareSupportClient client;
    private final WelfareSupportSaveService saveService;

    public int synchronize() {
        return saveService.saveAll(client.fetchAll());
    }
}
