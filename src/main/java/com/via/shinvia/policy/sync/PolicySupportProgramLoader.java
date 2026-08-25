package com.via.shinvia.policy.sync;

import com.via.shinvia.policy.loan.service.PolicySupportSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
// 서버 시작 시 정책상품 동기화 기능
public class PolicySupportProgramLoader
        implements ApplicationRunner {

    private final PolicySupportSyncService syncService;

    @Value("${finance.api.sync-on-startup:false}")
    private boolean syncOnStartup;

    @Override
    public void run(
            ApplicationArguments args
    ) {
        if (!syncOnStartup) {
            return;
        }

        try {
            int count =
                    syncService.synchronize();

            log.info(
                    "정책상품 API 자동 동기화 성공: {}건",
                    count
            );

        } catch (Exception e) {

            /*
             * API 오류가 발생해도
             * 서버 전체를 종료하지 않는다.
             */
            log.error(
                    "정책상품 API 자동 동기화 실패",
                    e
            );
        }
    }
}
