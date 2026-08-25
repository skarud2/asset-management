package com.via.shinvia.policy.social.service;

import com.via.shinvia.policy.social.client.SocialFinanceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 사회연대금융상품 API 동기화 기능
public class SocialFinanceSyncService {
    private final SocialFinanceClient client;
    private final SocialFinanceSaveService saveService;

    public int synchronize() {
        return saveService.saveAll(client.fetchAll());
    }
}
