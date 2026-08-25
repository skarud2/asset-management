package com.via.shinvia.policy.welfare.client;

import com.via.shinvia.policy.common.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
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
// 복합지원상품 API 동기화 기능
public class WelfareSupportClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    @Value("${finance.api.max-pages:1000}")
    private int maxPages;

    public List<WelfareSupportProduct> fetchAll() {
        List<WelfareSupportProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.WELFARE,
                    "", pageNumber, FETCH_SIZE);
            if (page.getProducts().isEmpty()) {
                break;
            }
            for (FinancialProductDTO product : page.getProducts()) {
                if (!StringUtils.hasText(product.getId())) {
                    log.warn("복합지원상품 상세 조회 생략: 상품 ID가 비어 있습니다. 상품명={}", product.getTitle());
                    continue;
                }
                result.add(toEntity(product, findDetail(product)));
            }
            pageNumber++;
        } while (!page.isLast() && pageNumber < maxPages);

        if (!page.isLast() && pageNumber >= maxPages) {
            log.warn("복합지원상품 조회를 최대 페이지에서 종료합니다. maxPages={}", maxPages);
        }

        return result;
    }

    private Map<String, String> findDetail(FinancialProductDTO product) {
        try {
            return client.findSourceDetail(
                    KinfaFinancialProductClient.ProductType.WELFARE, product.getId());
        } catch (RuntimeException e) {
            log.warn("복합지원상품 상세 조회 실패, 목록 정보로 저장합니다. productId={}", product.getId(), e);
            return Map.of();
        }
    }

    private WelfareSupportProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return WelfareSupportProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "spprtBizNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "fndtnNm"), item.getInstitution()), 150))
                .supportTarget(join(first(detail, "trgtSttn"), first(detail, "detlSttn"),
                        item.getFirstValue()))
                .ageCondition(limit(join(first(detail, "age", "agegrp"), item.getSecondValue()), 200))
                .welfareType(limit(first(detail, "welfareType", "clsf"), 150))
                .supportContent(first(detail, "detlCtns"))
                .applicationMethod(join(first(detail, "aplyMthod"), first(detail, "rprsTelno")))
                .responsibleInstitution(limit(first(detail, "fndtnNm"), 200))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
