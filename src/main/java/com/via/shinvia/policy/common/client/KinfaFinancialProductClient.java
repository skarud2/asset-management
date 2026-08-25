package com.via.shinvia.policy.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.via.shinvia.policy.common.dto.FinancialProductDTO;
import com.via.shinvia.policy.common.dto.FinancialProductPageDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// 서민금융진흥원 상품 동기화 API 호출 기능
public class KinfaFinancialProductClient {
    private static final String BASE_URL = "https://www.kinfa.or.kr/financialProduct/";
    private static final Pattern WELFARE_CARD_PATTERN = Pattern.compile(
            "<p class=\"card-tit\">(.*?)</p>.*?" +
                    "<span class=\"dd fc-pri\">(.*?)</span>.*?" +
                    "<span class=\"dd fc-pri\">(.*?)</span>.*?" +
                    "<span class=\"dd\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "allCount\"\\)\\.text\\(\"(\\d+)\"\\)");
    private static final Pattern WELFARE_ID_PATTERN = Pattern.compile(
            "name=\"sn\"\\s+value=\"([^\"]+)\"");

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KinfaFinancialProductClient(@Qualifier("policyRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public FinancialProductPageDTO findProducts(
            ProductType productType, String keyword, int page, int size
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("selectKeyword1", "all");
        request.put("searchKeyword1", keyword == null ? "" : keyword.trim());
        request.put("recordCountPerPage", size);
        request.put("currentPageNo", page + 1);
        request.put("sortGbn", "");
        request.put("prdDs", productType.productCode);

        String response = restClient.post()
                .uri(BASE_URL + productType.endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("서민금융 상품 응답이 비어 있습니다.");
        }
        return productType == ProductType.WELFARE
                ? parseWelfare(response, page, size)
                : parseJson(response, productType, page, size);
    }

    // 상품 동기화용 상세 원본 필드 조회
    public Map<String, String> findSourceDetail(ProductType productType, String id) {
        String response = restClient.post()
                .uri(BASE_URL + productType.detailEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sn", id))
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("상품 상세정보를 찾을 수 없습니다.");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode item = root.isArray() && !root.isEmpty() ? root.get(0) : root;
            Map<String, String> result = new LinkedHashMap<>();
            item.fields().forEachRemaining(entry -> result.put(
                    entry.getKey(), entry.getValue().isNull() ? "" : entry.getValue().asText()));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("상품 상세정보 원본 변환에 실패했습니다.", e);
        }
    }

    private FinancialProductPageDTO parseJson(
            String response, ProductType productType, int page, int size
    ) {
        try {
            JsonNode root = objectMapper.readTree(response);
            List<FinancialProductDTO> products = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    products.add(productType == ProductType.ASSET
                            ? assetProduct(item) : socialProduct(item));
                }
            }
            long total = root.isEmpty() ? 0 : parseLong(text(root.get(0), "countAll"));
            return page(products, page, size, total == 0 ? products.size() : total);
        } catch (Exception e) {
            throw new IllegalStateException("서민금융 상품 JSON 변환에 실패했습니다.", e);
        }
    }

    private FinancialProductDTO assetProduct(JsonNode item) {
        return FinancialProductDTO.builder()
                .id(text(item, "sn")).title(text(item, "fincPrdNm"))
                .firstValue(text(item, "svnAmt"))
                .secondValue(text(item, "highestInrtMxmMtcnAmt"))
                .institution(text(item, "trtmInsttVal")).build();
    }

    private FinancialProductDTO socialProduct(JsonNode item) {
        return FinancialProductDTO.builder()
                .id(text(item, "gno")).title(text(item, "spprtIsttNm"))
                .firstValue(text(item, "spprtTrgt"))
                .secondValue(text(item, "clsf"))
                .institution(text(item, "ofrInsttNm")).build();
    }

    private FinancialProductPageDTO parseWelfare(String response, int page, int size) {
        List<String> ids = new ArrayList<>();
        Matcher idMatcher = WELFARE_ID_PATTERN.matcher(response);
        while (idMatcher.find()) ids.add(idMatcher.group(1));

        List<FinancialProductDTO> products = new ArrayList<>();
        Matcher cards = WELFARE_CARD_PATTERN.matcher(response);
        while (cards.find()) {
            String id = ids.size() > products.size() ? ids.get(products.size()) : "";
            products.add(FinancialProductDTO.builder()
                    .id(id).title(clean(cards.group(1)))
                    .firstValue(clean(cards.group(2))).secondValue(clean(cards.group(3)))
                    .institution(clean(cards.group(4))).build());
        }
        Matcher totalMatcher = TOTAL_PATTERN.matcher(response);
        long total = totalMatcher.find() ? parseLong(totalMatcher.group(1)) : products.size();
        return page(products, page, size, total);
    }

    private FinancialProductPageDTO page(
            List<FinancialProductDTO> products, int page, int size, long total
    ) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return FinancialProductPageDTO.builder()
                .products(products).page(page).size(size).totalElements(total)
                .totalPages(totalPages).first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1).build();
    }

    private String text(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String clean(String value) {
        return HtmlUtils.htmlUnescape(value)
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ").trim();
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public enum ProductType {
        ASSET("fineUsePropProdList.do", "fineUsePropProdListDtl.do", "2"),
        SOCIAL("fineUseFinanceList.do", "fineUseFinanceListDtl.do", "3"),
        WELFARE("fineUseWelfareProdList.do", "fineUseWelfareListDtl.do", "4");

        private final String endpoint;
        private final String detailEndpoint;
        private final String productCode;

        ProductType(String endpoint, String detailEndpoint, String productCode) {
            this.endpoint = endpoint;
            this.detailEndpoint = detailEndpoint;
            this.productCode = productCode;
        }
    }
}
