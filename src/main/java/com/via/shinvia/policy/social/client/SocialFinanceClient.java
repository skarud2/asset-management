package com.via.shinvia.policy.social.client;

import com.via.shinvia.policy.common.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.social.entity.SocialFinanceProduct;
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
// 사회연대금융상품 API 동기화 기능
public class SocialFinanceClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    @Value("${finance.api.max-pages:1000}")
    private int maxPages;

    public List<SocialFinanceProduct> fetchAll() {
        List<SocialFinanceProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.SOCIAL,
                    "", pageNumber, FETCH_SIZE);
            if (page.getProducts().isEmpty()) {
                break;
            }
            for (FinancialProductDTO product : page.getProducts()) {
                if (!StringUtils.hasText(product.getId())) {
                    log.warn("사회연대금융상품 상세 조회 생략: 상품 ID가 비어 있습니다. 상품명={}", product.getTitle());
                    continue;
                }
                result.add(toEntity(product, findDetail(product)));
            }
            pageNumber++;
        } while (!page.isLast() && pageNumber < maxPages);

        if (!page.isLast() && pageNumber >= maxPages) {
            log.warn("사회연대금융상품 조회를 최대 페이지에서 종료합니다. maxPages={}", maxPages);
        }

        return result;
    }

    private Map<String, String> findDetail(FinancialProductDTO product) {
        try {
            return client.findSourceDetail(
                    KinfaFinancialProductClient.ProductType.SOCIAL, product.getId());
        } catch (RuntimeException e) {
            log.warn("사회연대금융상품 상세 조회 실패, 목록 정보로 저장합니다. productId={}", product.getId(), e);
            return Map.of();
        }
    }

    private SocialFinanceProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return SocialFinanceProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "spprtIsttNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "ofrInsttNm"), item.getInstitution()), 150))
                .productCategory(limit(first(detail, "clsf"), 100))
                .supportTarget(join(first(detail, "spprtTrgt"), first(detail, "spprtTrgtDetlCnd")))
                .businessType(limit(join(first(detail, "projOverview"), first(detail, "rcritSchdl")), 200))
                .supportMethod(join(first(detail, "aplyMthod"), first(detail, "oprInstt")))
                .supportAmount(limit(first(detail, "spprtAmt", "lonNedAmt"), 200))
                .handlingInstitution(limit(join(first(detail, "mngeInstt"), first(detail, "oprInstt")), 200))
                .applicationMethod(join(first(detail, "aplyMthod"), first(detail, "inqy"),
                        first(detail, "etcNoitm")))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
