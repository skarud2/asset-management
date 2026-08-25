package com.via.shinvia.welfare.client;

import com.via.shinvia.welfare.dto.BokjiroServiceDetailDto;
import com.via.shinvia.welfare.dto.BokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.BokjiroApiResponse;
import com.via.shinvia.welfare.dto.BokjiroServiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BokjiroApiClient {

    private final RestClient restClient;

    @Value("${bokjiro.api.base-url:https://apis.data.go.kr/B554287/NationalWelfareInformationsV001}")
    private String baseUrl;

    @Value("${bokjiro.api.service-key:}")
    private String serviceKey;

    public BokjiroApiClient() {
        this.restClient = RestClient.create();
    }

    /**
     * 중앙부처 복지서비스 전체 목록 반복 조회 (스프링부트 시작 시 DB 저장용)
     */
    public List<BokjiroServiceItem> fetchAllWelfareList() {
        List<BokjiroServiceItem> allItems = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100;
        int totalCount = Integer.MAX_VALUE;
        int maxPages = 100;

        while (allItems.size() < totalCount && pageNo <= maxPages) {
            BokjiroListRequestDTO request = BokjiroListRequestDTO.builder()
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .build();

            BokjiroApiResponse response = fetchWelfareList(request);
            if (response == null || response.getServList() == null || response.getServList().isEmpty()) {
                break;
            }

            allItems.addAll(response.getServList());
            totalCount = response.getTotalCount();

            log.info("복지로 API 중앙부처 복지서비스 동기화 - pageNo={}, 현재={}건, 전체={}건",
                    pageNo, allItems.size(), totalCount);

            if (response.getServList().size() < numOfRows) {
                break;
            }
            pageNo++;
        }

        return allItems;
    }

    /**
     * 지자체 복지서비스 전체 목록 반복 조회 (LocalWelfareInformationsV001)
     */
    public List<BokjiroServiceItem> fetchAllLocalWelfareList() {
        List<BokjiroServiceItem> allItems = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100;
        int totalCount = Integer.MAX_VALUE;
        int maxPages = 100;

        String localUrl = "https://apis.data.go.kr/B554287/LocalWelfareInformationsV001/LocalWelfarelistV001";

        while (allItems.size() < totalCount && pageNo <= maxPages) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(localUrl)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("callTp", "L")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("srchKeyCode", "001");

                URI uri = buildUri(builder);

                log.info("복지로 지자체 목록 API 호출 URI: {}", uri);

                byte[] responseBytes = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(byte[].class);

                if (responseBytes == null || responseBytes.length == 0) {
                    break;
                }

                String xml = new String(responseBytes, StandardCharsets.UTF_8);
                BokjiroApiResponse response = parseListXml(xml);

                if (response == null || response.getServList() == null || response.getServList().isEmpty()) {
                    break;
                }

                allItems.addAll(response.getServList());
                totalCount = response.getTotalCount();

                log.info("복지로 API 지자체 복지서비스 동기화 - pageNo={}, 현재={}건, 전체={}건",
                        pageNo, allItems.size(), totalCount);

                if (response.getServList().size() < numOfRows) {
                    break;
                }
                pageNo++;
            } catch (Exception e) {
                log.warn("지자체 복지서비스 API 수신 중 안내 (공공데이터포털에서 '지자체 복지서비스 Open API' 활용신청 필요): {}", e.getMessage());
                break;
            }
        }

        return allItems;
    }

    /**
     * 복지서비스 목록 조회 (/NationalWelfarelistV001)
     */
    public BokjiroApiResponse fetchWelfareList(BokjiroListRequestDTO request) {
        String listUrl = resolveListUrl();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(listUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("callTp", "L")
                .queryParam("pageNo", request.getPageNo() <= 0 ? 1 : request.getPageNo())
                .queryParam("numOfRows", request.getNumOfRows() <= 0 ? 10 : request.getNumOfRows());

        String srchKeyCode = (request.getSrchKeyCode() != null && !request.getSrchKeyCode().isBlank())
                ? request.getSrchKeyCode() : "001";
        builder.queryParam("srchKeyCode", srchKeyCode);
        if (request.getSearchWrd() != null && !request.getSearchWrd().isBlank()) {
            builder.queryParam("searchWrd", request.getSearchWrd());
        }
        if (request.getLifeArray() != null && !request.getLifeArray().isBlank()) {
            builder.queryParam("lifeArray", request.getLifeArray());
        }
        if (request.getTrgterIndvdlArray() != null && !request.getTrgterIndvdlArray().isBlank()) {
            builder.queryParam("trgterIndvdlArray", request.getTrgterIndvdlArray());
        }
        if (request.getIntrsThemaArray() != null && !request.getIntrsThemaArray().isBlank()) {
            builder.queryParam("intrsThemaArray", request.getIntrsThemaArray());
        }
        if (request.getAge() != null) {
            builder.queryParam("age", request.getAge());
        }
        if (request.getOnapPsbltYn() != null && !request.getOnapPsbltYn().isBlank()) {
            builder.queryParam("onapPsbltYn", request.getOnapPsbltYn());
        }
        if (request.getOrderBy() != null && !request.getOrderBy().isBlank()) {
            builder.queryParam("orderBy", request.getOrderBy());
        }

        URI uri = buildUri(builder);

        log.info("복지로 목록 API 호출 URI: {}", uri);

        byte[] responseBytes = restClient.get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);

        if (responseBytes == null || responseBytes.length == 0) {
            throw new IllegalStateException("복지로 API 응답이 비어있습니다.");
        }

        String xml = new String(responseBytes, StandardCharsets.UTF_8);
        return parseListXml(xml);
    }

    /**
     * 복지서비스 상세 조회 (/NationalWelfaredetailedV001)
     */
    public BokjiroServiceDetailDto fetchWelfareDetail(String servId) {
        String detailUrl = resolveDetailUrl();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(detailUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("callTp", "D")
                .queryParam("servId", servId);

        URI uri = buildUri(builder);

        log.info("복지로 상세 API 호출 URI: servId={}, uri={}", servId, uri);

        byte[] responseBytes = restClient.get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);

        if (responseBytes == null || responseBytes.length == 0) {
            throw new IllegalStateException("복지로 상세 API 응답이 비어있습니다.");
        }

        String xml = new String(responseBytes, StandardCharsets.UTF_8);
        return parseDetailXml(xml);
    }

    private URI buildUri(UriComponentsBuilder builder) {
        boolean isAlreadyEncoded = serviceKey != null && serviceKey.contains("%");
        return builder.build(isAlreadyEncoded).toUri();
    }

    private BokjiroApiResponse parseListXml(String xml) {
        try {
            Document document = parseXmlDocument(xml);
            document.getDocumentElement().normalize();

            String returnAuthMsg = getNodeText(document, "returnAuthMsg");
            String errMsg = getNodeText(document, "errMsg");
            String returnReasonCode = getNodeText(document, "returnReasonCode");
            if (returnAuthMsg != null || errMsg != null) {
                log.error("복지로 API 공공데이터 오류 응답 수신: [코드={}] {}, 원본XML:\n{}", returnReasonCode, returnAuthMsg, xml);
                throw new IllegalStateException("복지로 API 인증/서비스 오류 [코드=" + returnReasonCode + "]: " + returnAuthMsg + " (" + errMsg + ")");
            }

            int totalCount = parseInteger(getNodeText(document, "totalCount"));
            int pageNo = parseInteger(getNodeText(document, "pageNo"));
            int numOfRows = parseInteger(getNodeText(document, "numOfRows"));
            String resultCode = getNodeText(document, "resultCode");
            String resultMessage = getNodeText(document, "resultMessage");

            List<BokjiroServiceItem> items = new ArrayList<>();
            NodeList itemNodes = document.getElementsByTagName("servList");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node node = itemNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element elem = (Element) node;

                items.add(BokjiroServiceItem.builder()
                        .servId(getElementText(elem, "servId"))
                        .servNm(getElementText(elem, "servNm"))
                        .jurMnofNm(getElementText(elem, "jurMnofNm"))
                        .jurOrgNm(getElementText(elem, "jurOrgNm"))
                        .inqNum(getElementText(elem, "inqNum"))
                        .servDgst(getElementText(elem, "servDgst"))
                        .servDtlLink(getElementText(elem, "servDtlLink"))
                        .svcfrstRegTs(getElementText(elem, "svcfrstRegTs"))
                        .lifeArray(getElementText(elem, "lifeArray"))
                        .intrsThemaArray(getElementText(elem, "intrsThemaArray"))
                        .trgterIndvdlArray(getElementText(elem, "trgterIndvdlArray"))
                        .sprtCycNm(getElementText(elem, "sprtCycNm"))
                        .srvPvsnNm(getElementText(elem, "srvPvsnNm"))
                        .rprsCtadr(getElementText(elem, "rprsCtadr"))
                        .onapPsbltYn(getElementText(elem, "onapPsbltYn"))
                        .build());
            }

            return BokjiroApiResponse.builder()
                    .totalCount(totalCount)
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .resultCode(resultCode)
                    .resultMessage(resultMessage)
                    .servList(items)
                    .build();

        } catch (Exception e) {
            log.error("복지로 목록 XML 파싱 실패. XML내용:\n{}", xml, e);
            throw new IllegalStateException("복지로 목록 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private BokjiroServiceDetailDto parseDetailXml(String xml) {
        try {
            Document document = parseXmlDocument(xml);
            document.getDocumentElement().normalize();

            String returnAuthMsg = getNodeText(document, "returnAuthMsg");
            String errMsg = getNodeText(document, "errMsg");
            String returnReasonCode = getNodeText(document, "returnReasonCode");
            if (returnAuthMsg != null || errMsg != null) {
                log.error("복지로 상세 API 공공데이터 오류 응답 수신: [코드={}] {}, 원본XML:\n{}", returnReasonCode, returnAuthMsg, xml);
                throw new IllegalStateException("복지로 상세 API 인증/서비스 오류 [코드=" + returnReasonCode + "]: " + returnAuthMsg + " (" + errMsg + ")");
            }

            return BokjiroServiceDetailDto.builder()
                    .servId(getNodeText(document, "servId"))
                    .servNm(getNodeText(document, "servNm"))
                    .jurMnofNm(getNodeText(document, "jurMnofNm"))
                    .jurOrgNm(getNodeText(document, "jurOrgNm"))
                    .wlfareInfoOutlCn(getNodeText(document, "wlfareInfoOutlCn"))
                    .crtrYr(getNodeText(document, "crtrYr"))
                    .rprsCtadr(getNodeText(document, "rprsCtadr"))
                    .sprtCycNm(getNodeText(document, "sprtCycNm"))
                    .srvPvsnNm(getNodeText(document, "srvPvsnNm"))
                    .lifeArray(getNodeText(document, "lifeArray"))
                    .intrsThemaArray(getNodeText(document, "intrsThemaArray"))
                    .trgterIndvdlArray(getNodeText(document, "trgterIndvdlArray"))
                    .tgtrDtlCn(getNodeText(document, "tgtrDtlCn"))
                    .slctCritCn(getNodeText(document, "slctCritCn"))
                    .alwServCn(getNodeText(document, "alwServCn"))
                    .applmetList(parseSubList(document, "applmetList"))
                    .inqplCtadrList(parseSubList(document, "inqplCtadrList"))
                    .inqplHmpgReldList(parseSubList(document, "inqplHmpgReldList"))
                    .basfrmList(parseSubList(document, "basfrmList"))
                    .baslawList(parseSubList(document, "baslawList"))
                    .build();

        } catch (Exception e) {
            log.error("복지로 상세 XML 파싱 실패. XML내용:\n{}", xml, e);
            throw new IllegalStateException("복지로 상세 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private List<BokjiroServiceDetailDto.DetailSubItem> parseSubList(Document document, String tagName) {
        List<BokjiroServiceDetailDto.DetailSubItem> list = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element elem = (Element) node;
            list.add(BokjiroServiceDetailDto.DetailSubItem.builder()
                    .servSeCode(getElementText(elem, "servSeCode"))
                    .servSeDetailNm(getElementText(elem, "servSeDetailNm"))
                    .servSeDetailLink(getElementText(elem, "servSeDetailLink"))
                    .build());
        }
        return list;
    }

    private Document parseXmlDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String getNodeText(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        return clean(list.item(0).getTextContent());
    }

    private String getElementText(Element elem, String tagName) {
        NodeList list = elem.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        return clean(list.item(0).getTextContent());
    }

    private String clean(String val) {
        if (val == null) return null;
        String trimmed = HtmlUtils.htmlUnescape(val).trim();
        return trimmed.isBlank() || "-".equals(trimmed) ? null : trimmed;
    }

    private int parseInteger(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String resolveListUrl() {
        if (baseUrl.contains("NationalWelfarelistV001")) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") ? baseUrl + "NationalWelfarelistV001" : baseUrl + "/NationalWelfarelistV001";
    }

    private String resolveDetailUrl() {
        if (baseUrl.contains("NationalWelfaredetailedV001")) {
            return baseUrl;
        }
        if (baseUrl.contains("NationalWelfarelistV001")) {
            return baseUrl.replace("NationalWelfarelistV001", "NationalWelfaredetailedV001");
        }
        return baseUrl.endsWith("/") ? baseUrl + "NationalWelfaredetailedV001" : baseUrl + "/NationalWelfaredetailedV001";
    }
}
