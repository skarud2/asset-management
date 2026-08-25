package com.via.shinvia.welfare.client;

import com.via.shinvia.welfare.dto.LocalBokjiroDetailResponseDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListResponseDTO;
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
public class LocalBokjiroApiClient {

    private final RestClient restClient;

    @Value("${localbokjiro.api.base-url:https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations}")
    private String baseUrl;

    @Value("${localbokjiro.api.service-key:}")
    private String serviceKey;

    public LocalBokjiroApiClient() {
        this.restClient = RestClient.create();
    }

    /**
     * 지자체 복지서비스 전체 목록 반복 조회
     */
    public List<LocalBokjiroListResponseDTO.LocalWelfareItem> fetchAllLocalWelfareList() {
        List<LocalBokjiroListResponseDTO.LocalWelfareItem> allItems = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100;
        int totalCount = Integer.MAX_VALUE;
        int maxPages = 100;

        while (allItems.size() < totalCount && pageNo <= maxPages) {
            LocalBokjiroListRequestDTO request = LocalBokjiroListRequestDTO.builder()
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .srchKeyCode("001")
                    .build();

            LocalBokjiroListResponseDTO response = fetchLocalWelfareList(request);
            if (response == null || response.getServList() == null || response.getServList().isEmpty()) {
                break;
            }

            allItems.addAll(response.getServList());
            totalCount = response.getTotalCount();

            log.info("localbokjiro API 지자체 복지서비스 동기화 - pageNo={}, 현재={}건, 전체={}건",
                    pageNo, allItems.size(), totalCount);

            if (response.getServList().size() < numOfRows) {
                break;
            }
            pageNo++;
        }

        return allItems;
    }

    /**
     * 지자체 복지서비스 목록 조회
     */
    public LocalBokjiroListResponseDTO fetchLocalWelfareList(LocalBokjiroListRequestDTO request) {
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
        if (request.getCtpvNm() != null && !request.getCtpvNm().isBlank()) {
            builder.queryParam("ctpvNm", request.getCtpvNm());
        }
        if (request.getSggNm() != null && !request.getSggNm().isBlank()) {
            builder.queryParam("sggNm", request.getSggNm());
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
        log.info("localbokjiro 목록 API 호출 URI: {}", uri);

        byte[] responseBytes = restClient.get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);

        if (responseBytes == null || responseBytes.length == 0) {
            throw new IllegalStateException("localbokjiro API 응답이 비어있습니다.");
        }

        String xml = new String(responseBytes, StandardCharsets.UTF_8);
        return parseListXml(xml);
    }

    /**
     * 지자체 복지서비스 상세 조회
     */
    public LocalBokjiroDetailResponseDTO fetchLocalWelfareDetail(String servId) {
        String detailUrl = resolveDetailUrl();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(detailUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("callTp", "D")
                .queryParam("servId", servId);

        URI uri = buildUri(builder);
        log.info("localbokjiro 상세 API 호출 URI: servId={}, uri={}", servId, uri);

        byte[] responseBytes = restClient.get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);

        if (responseBytes == null || responseBytes.length == 0) {
            throw new IllegalStateException("localbokjiro 상세 API 응답이 비어있습니다.");
        }

        String xml = new String(responseBytes, StandardCharsets.UTF_8);
        return parseDetailXml(xml);
    }

    private URI buildUri(UriComponentsBuilder builder) {
        boolean isAlreadyEncoded = serviceKey != null && serviceKey.contains("%");
        return builder.build(isAlreadyEncoded).toUri();
    }

    private String resolveListUrl() {
        if (baseUrl.endsWith("/LcgvWelfarelist")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "LcgvWelfarelist";
        }
        return baseUrl + "/LcgvWelfarelist";
    }

    private String resolveDetailUrl() {
        if (baseUrl.endsWith("/LcgvWelfaredetailed")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/LcgvWelfarelist")) {
            return baseUrl.replace("/LcgvWelfarelist", "/LcgvWelfaredetailed");
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "LcgvWelfaredetailed";
        }
        return baseUrl + "/LcgvWelfaredetailed";
    }

    private LocalBokjiroListResponseDTO parseListXml(String xml) {
        try {
            Document document = parseXmlDocument(xml);
            document.getDocumentElement().normalize();

            String returnAuthMsg = getNodeText(document, "returnAuthMsg");
            String errMsg = getNodeText(document, "errMsg");
            String returnReasonCode = getNodeText(document, "returnReasonCode");
            if (returnAuthMsg != null || errMsg != null) {
                log.error("localbokjiro API 공공데이터 오류 응답 수신: [코드={}] {}, 원본XML:\n{}", returnReasonCode, returnAuthMsg, xml);
                throw new IllegalStateException("localbokjiro API 인증/서비스 오류 [코드=" + returnReasonCode + "]: " + returnAuthMsg + " (" + errMsg + ")");
            }

            int totalCount = parseInteger(getNodeText(document, "totalCount"));
            int pageNo = parseInteger(getNodeText(document, "pageNo"));
            int numOfRows = parseInteger(getNodeText(document, "numOfRows"));
            String resultCode = getNodeText(document, "resultCode");
            String resultMessage = getNodeText(document, "resultMessage");

            List<LocalBokjiroListResponseDTO.LocalWelfareItem> items = new ArrayList<>();
            NodeList itemNodes = document.getElementsByTagName("servList");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node node = itemNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element elem = (Element) node;

                items.add(LocalBokjiroListResponseDTO.LocalWelfareItem.builder()
                        .servId(getElementText(elem, "servId"))
                        .servNm(getElementText(elem, "servNm"))
                        .jurMnofNm(getElementText(elem, "jurMnofNm"))
                        .jurOrgNm(getElementText(elem, "jurOrgNm"))
                        .ctpvNm(getElementText(elem, "ctpvNm"))
                        .sggNm(getElementText(elem, "sggNm"))
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

            return LocalBokjiroListResponseDTO.builder()
                    .totalCount(totalCount)
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .resultCode(resultCode)
                    .resultMessage(resultMessage)
                    .servList(items)
                    .build();

        } catch (Exception e) {
            log.error("localbokjiro 목록 XML 파싱 실패. XML내용:\n{}", xml, e);
            throw new IllegalStateException("localbokjiro 목록 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private LocalBokjiroDetailResponseDTO parseDetailXml(String xml) {
        try {
            Document document = parseXmlDocument(xml);
            document.getDocumentElement().normalize();

            String returnAuthMsg = getNodeText(document, "returnAuthMsg");
            String errMsg = getNodeText(document, "errMsg");
            String returnReasonCode = getNodeText(document, "returnReasonCode");
            if (returnAuthMsg != null || errMsg != null) {
                log.error("localbokjiro 상세 API 공공데이터 오류 응답 수신: [코드={}] {}, 원본XML:\n{}", returnReasonCode, returnAuthMsg, xml);
                throw new IllegalStateException("localbokjiro 상세 API 인증/서비스 오류 [코드=" + returnReasonCode + "]: " + returnAuthMsg + " (" + errMsg + ")");
            }

            return LocalBokjiroDetailResponseDTO.builder()
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
            log.error("localbokjiro 상세 XML 파싱 실패. XML내용:\n{}", xml, e);
            throw new IllegalStateException("localbokjiro 상세 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private List<LocalBokjiroDetailResponseDTO.DetailSubItem> parseSubList(Document document, String tagName) {
        List<LocalBokjiroDetailResponseDTO.DetailSubItem> list = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element elem = (Element) node;
            list.add(LocalBokjiroDetailResponseDTO.DetailSubItem.builder()
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
}
