package com.via.shinvia.policy.sync;

import com.via.shinvia.policy.asset.service.AssetProductSyncService;
import com.via.shinvia.policy.social.service.SocialFinanceSyncService;
import com.via.shinvia.policy.welfare.service.WelfareSupportSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
// 세 정책금융상품 시작 동기화 기능
public class FinancialProductSyncLoader implements ApplicationRunner {
    private final AssetProductSyncService assetSyncService;
    private final SocialFinanceSyncService socialSyncService;
    private final WelfareSupportSyncService welfareSyncService;

    @Value("${finance.api.sync-on-startup:false}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            return;
        }

        synchronize("자산형성상품", assetSyncService::synchronize);
        synchronize("사회연대금융", socialSyncService::synchronize);
        synchronize("복합지원", welfareSyncService::synchronize);
    }

    private void synchronize(String name, SyncAction action) {
        try {
            log.info("{} 동기화 완료: {}건", name, action.run());
        } catch (Exception e) {
            log.error("{} 동기화 실패", name, e);
        }
    }

    @FunctionalInterface
    private interface SyncAction {
        int run();
    }
}
