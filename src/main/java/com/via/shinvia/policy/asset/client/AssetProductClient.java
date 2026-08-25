package com.via.shinvia.policy.asset.client;

import com.via.shinvia.policy.asset.entity.AssetFormationProduct;
import com.via.shinvia.policy.common.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.via.shinvia.policy.common.util.PolicyProductValues.*;

@Component
@RequiredArgsConstructor
@Slf4j
// 자산형성상품 API 동기화 기능
public class AssetProductClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    @Value("${finance.api.max-pages:1000}")
    private int maxPages;

    public List<AssetFormationProduct> fetchAll() {
        List<AssetFormationProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.ASSET,
                    "", pageNumber, FETCH_SIZE);
            if (page.getProducts().isEmpty()) {
                break;
            }
            for (FinancialProductDTO product : page.getProducts()) {
                if (!StringUtils.hasText(product.getId())) {
                    log.warn("자산형성상품 상세 조회 생략: 상품 ID가 비어 있습니다. 상품명={}", product.getTitle());
                    continue;
                }
                result.add(toEntity(product, findDetail(product)));
            }
            pageNumber++;
        } while (!page.isLast() && pageNumber < maxPages);

        if (!page.isLast() && pageNumber >= maxPages) {
            log.warn("자산형성상품 조회를 최대 페이지에서 종료합니다. maxPages={}", maxPages);
        }

        return result;
    }

    private Map<String, String> findDetail(FinancialProductDTO product) {
        try {
            return client.findSourceDetail(
                    KinfaFinancialProductClient.ProductType.ASSET, product.getId());
        } catch (RuntimeException e) {
            log.warn("자산형성상품 상세 조회 실패, 목록 정보로 저장합니다. productId={}", product.getId(), e);
            return Map.of();
        }
    }

    private AssetFormationProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return AssetFormationProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "fincPrdNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "ofrInsttNm"), item.getInstitution()), 150))
                .subscriptionTarget(join(first(detail, "trgt"), first(detail, "spprtSpprtDetlCnd")))
                .subscriptionPeriod(limit(first(detail, "joinPrid"), 100))
                .incomeCondition(first(detail, "incmeRcgnzAmnt"))
                .ageCondition(limit(first(detail, "age"), 200))
                .supportRegion(limit(first(detail, "rsdnZone"), 200))
                .savingMethod(join(first(detail, "ipawy"), first(detail, "prdDs")))
                .governmentSupport(limit(join(first(detail, "svnAmt"),
                        first(detail, "highestInrtMxmMtcnAmt")), 200))
                .maturityBenefit(join(first(detail, "prdChrct"), first(detail, "etcNoitm")))
                .applicationMethod(join(first(detail, "etcMthod"),
                        first(detail, "trtmInsttVal"), first(detail, "inqy")))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
