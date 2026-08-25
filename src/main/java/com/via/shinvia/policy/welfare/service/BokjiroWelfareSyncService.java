package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.welfare.client.BokjiroApiClient;
import com.via.shinvia.welfare.client.LocalBokjiroApiClient;
import com.via.shinvia.welfare.dto.BokjiroServiceItem;
import com.via.shinvia.welfare.dto.LocalBokjiroListResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BokjiroWelfareSyncService {

    private static final String SOURCE_NATIONAL = "BOKJIRO_NATIONAL";
    private static final String SOURCE_LOCAL = "BOKJIRO_LOCAL";

    private final BokjiroApiClient bokjiroApiClient;
    private final LocalBokjiroApiClient localBokjiroApiClient;
    private final BokjiroWelfareNormalizeService normalizeService;
    private final WelfareSupportSaveService saveService;

    public int synchronizeNational() {
        List<BokjiroServiceItem> items =
                bokjiroApiClient.fetchAllWelfareList();

        List<WelfareSupportProduct> products = items.stream()
                .filter(item -> item.getServId() != null)
                .filter(item -> item.getServNm() != null)
                .map(normalizeService::fromNational)
                .toList();

        int savedCount = saveService.saveAllBySourceType(
                SOURCE_NATIONAL,
                products
        );

        log.info("복지로 중앙부처 복지서비스 동기화 완료: {}건", savedCount);
        return savedCount;
    }

    public int synchronizeLocal() {
        List<LocalBokjiroListResponseDTO.LocalWelfareItem> items =
                localBokjiroApiClient.fetchAllLocalWelfareList();

        List<WelfareSupportProduct> products = items.stream()
                .filter(item -> item.getServId() != null)
                .filter(item -> item.getServNm() != null)
                .map(normalizeService::fromLocal)
                .toList();

        int savedCount = saveService.saveAllBySourceType(
                SOURCE_LOCAL,
                products
        );

        log.info("복지로 지자체 복지서비스 동기화 완료: {}건", savedCount);
        return savedCount;
    }

    public int synchronizeAll() {
        return synchronizeNational() + synchronizeLocal();
    }
}
