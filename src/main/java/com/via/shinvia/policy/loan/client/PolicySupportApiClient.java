package com.via.shinvia.policy.loan.client;

import com.via.shinvia.policy.loan.dto.api.LoanProductApiItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.client.RestClient;
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
// 맞춤대출 공공데이터 API 호출 기능
public class PolicySupportApiClient {

    private final RestClient restClient;

    // Bean 충돌로 인한 생성자 방식으로 변경,  @Qualifier
    public PolicySupportApiClient(
            @Qualifier("policyRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Value("${finance.api.service-key}")
    private String serviceKey;

    @Value("${finance.api.base-url}")
    private String baseUrl;

    @Value("${finance.api.num-of-rows:100}")
    private int numOfRows;

    @Value("${finance.api.max-pages:1000}")
    private int maxPages;

    /**
     * API의 모든 페이지를 반복 호출한다.
     */
    public List<LoanProductApiItem> fetchAll() {

        List<LoanProductApiItem> allItems = new ArrayList<>();

        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while (allItems.size() < totalCount
                && pageNo <= maxPages) {

            String xml = requestPage(pageNo);

            ApiPageResult pageResult = parseXml(xml);

            if (!"00".equals(pageResult.resultCode())) {
                throw new IllegalStateException(
                        "공공데이터 API 오류: "
                                + pageResult.resultCode()
                                + " / "
                                + pageResult.resultMessage()
                );
            }

            List<LoanProductApiItem> currentItems =
                    pageResult.items();

            if (currentItems.isEmpty()) {
                break;
            }

            allItems.addAll(currentItems);

            totalCount = pageResult.totalCount();

            log.info(
                    "정책상품 API 조회 - pageNo={}, 현재={}건, 전체={}건",
                    pageNo,
                    allItems.size(),
                    totalCount
            );

            pageNo++;
        }

        if (allItems.size() < totalCount
                && pageNo > maxPages) {
            log.warn(
                    "정책상품 API 최대 페이지 제한 도달 - maxPages={}, 조회={}건, 전체={}건",
                    maxPages,
                    allItems.size(),
                    totalCount
            );
        }

        return allItems;
    }

    /**
     * API 한 페이지 호출
     */
    /**
     * 공공데이터 API 한 페이지 호출
     *
     * 응답을 String으로 바로 받으면 서버의 Content-Type charset 설정에 따라
     * 한글이 ISO-8859-1 등으로 잘못 해석될 수 있다.
     *
     * 따라서 byte[]로 받은 뒤 UTF-8로 직접 변환한다.
     */
    private String requestPage(int pageNo) {

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "xml")
                .build()
                .toUri();

        byte[] responseBytes = restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);

        if (responseBytes == null
                || responseBytes.length == 0) {

            throw new IllegalStateException(
                    "공공데이터 API 응답이 비어 있습니다."
            );
        }

        String response =
                new String(
                        responseBytes,
                        StandardCharsets.UTF_8
                );

        log.debug(
                "정책상품 API 응답 수신 - pageNo={}, byteSize={}",
                pageNo,
                responseBytes.length
        );

        return response;
    }

    /**
     * XML 문자열을 DTO 목록으로 변환한다.
     */
    private ApiPageResult parseXml(String xml) {

        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            // XXE 공격 방지
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document = builder.parse(
                    new InputSource(
                            new StringReader(xml)
                    )
            );

            document
                    .getDocumentElement()
                    .normalize();

            String resultCode =
                    getDocumentText(
                            document,
                            "resultCode"
                    );

            String resultMessage =
                    getDocumentText(
                            document,
                            "resultMsg"
                    );

            int totalCount =
                    parseInteger(
                            getDocumentText(
                                    document,
                                    "totalCount"
                            )
                    );

            NodeList itemNodes =
                    document.getElementsByTagName(
                            "item"
                    );

            List<LoanProductApiItem> items =
                    new ArrayList<>();

            for (int i = 0;
                 i < itemNodes.getLength();
                 i++) {

                Node node =
                        itemNodes.item(i);

                if (node.getNodeType()
                        != Node.ELEMENT_NODE) {
                    continue;
                }

                Element itemElement =
                        (Element) node;

                LoanProductApiItem item =
                        LoanProductApiItem.builder()

                                .seq(
                                        text(
                                                itemElement,
                                                "seq"
                                        )
                                )

                                .financialProductName(
                                        text(
                                                itemElement,
                                                "finprdnm"
                                        )
                                )

                                .loanLimit(
                                        text(
                                                itemElement,
                                                "lnlmt"
                                        )
                                )

                                .interestRateCategory(
                                        text(
                                                itemElement,
                                                "irtCtg"
                                        )
                                )

                                .interestRate(
                                        text(
                                                itemElement,
                                                "irt"
                                        )
                                )

                                .maxTotalLoanTerm(
                                        text(
                                                itemElement,
                                                "maxtotlntrm"
                                        )
                                )

                                .maxDeferredTerm(
                                        text(
                                                itemElement,
                                                "maxdfrmtrm"
                                        )
                                )

                                .maxRepaymentTerm(
                                        text(
                                                itemElement,
                                                "maxrdpttrm"
                                        )
                                )

                                .repaymentMethod(
                                        text(
                                                itemElement,
                                                "rdptmthd"
                                        )
                                )

                                .usage(
                                        text(
                                                itemElement,
                                                "usge"
                                        )
                                )

                                .target(
                                        text(
                                                itemElement,
                                                "trgt"
                                        )
                                )

                                .institutionCategory(
                                        text(
                                                itemElement,
                                                "instCtg"
                                        )
                                )

                                .offeringInstitutionName(
                                        text(
                                                itemElement,
                                                "ofrinstnm"
                                        )
                                )

                                .supportArea(
                                        text(
                                                itemElement,
                                                "rsdAreaPamtEqltIstm"
                                        )
                                )

                                .supportTargetDetailCondition(
                                        text(
                                                itemElement,
                                                "suprtgtdtlcond"
                                        )
                                )

                                .age(
                                        text(
                                                itemElement,
                                                "age"
                                        )
                                )

                                .income(
                                        text(
                                                itemElement,
                                                "incm"
                                        )
                                )

                                .residenceArea(
                                        text(
                                                itemElement,
                                                "rsdarea"
                                        )
                                )

                                .creditScore(
                                        text(
                                                itemElement,
                                                "crdtsc"
                                        )
                                )

                                .householdCondition(
                                        text(
                                                itemElement,
                                                "housholdcnt"
                                        )
                                )

                                .referenceContact(
                                        text(
                                                itemElement,
                                                "rfrccnpl"
                                        )
                                )

                                .guaranteeInstitution(
                                        text(
                                                itemElement,
                                                "grninst"
                                        )
                                )

                                .joinMethod(
                                        text(
                                                itemElement,
                                                "jnmthd"
                                        )
                                )

                                .repaymentFee(
                                        text(
                                                itemElement,
                                                "rpymdcfe"
                                        )
                                )

                                .loanIncidentalCost(
                                        text(
                                                itemElement,
                                                "lnicdcst"
                                        )
                                )

                                .overdueInterestRate(
                                        text(
                                                itemElement,
                                                "ovitryr"
                                        )
                                )

                                .preferentialInterestCondition(
                                        text(
                                                itemElement,
                                                "prftaddirtcond"
                                        )
                                )

                                .etcReference(
                                        text(
                                                itemElement,
                                                "etcrefsbjc"
                                        )
                                )

                                .handlingInstitution(
                                        text(
                                                itemElement,
                                                "hdlinst"
                                        )
                                )

                                .contact(
                                        text(
                                                itemElement,
                                                "cnpl"
                                        )
                                )

                                .relatedSite(
                                        text(
                                                itemElement,
                                                "rltsite"
                                        )
                                )

                                .targetFilter(
                                        text(
                                                itemElement,
                                                "tgtFltr"
                                        )
                                )

                                .handlingInstitutionDetail(
                                        text(
                                                itemElement,
                                                "hdlinstdtlvw"
                                        )
                                )

                                .productCategory(
                                        text(
                                                itemElement,
                                                "prdCtg"
                                        )
                                )

                                .productOperationPeriod(
                                        text(
                                                itemElement,
                                                "prdoprprid"
                                        )
                                )

                                .financialEducationProductYn(
                                        text(
                                                itemElement,
                                                "kinfaprdyn"
                                        )
                                )

                                .financialEducationProductEtc(
                                        text(
                                                itemElement,
                                                "kinfaprdetc"
                                        )
                                )

                                .build();

                items.add(item);
            }

            return new ApiPageResult(
                    resultCode,
                    resultMessage,
                    totalCount,
                    items
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "공공데이터 XML 파싱 실패",
                    e
            );
        }
    }

    private String text(
            Element parent,
            String tagName
    ) {
        NodeList nodes =
                parent.getElementsByTagName(
                        tagName
                );

        if (nodes.getLength() == 0) {
            return null;
        }

        return clean(
                nodes
                        .item(0)
                        .getTextContent()
        );
    }

    private String getDocumentText(
            Document document,
            String tagName
    ) {
        NodeList nodes =
                document.getElementsByTagName(
                        tagName
                );

        if (nodes.getLength() == 0) {
            return null;
        }

        return clean(
                nodes
                        .item(0)
                        .getTextContent()
        );
    }

    private String clean(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = HtmlUtils.htmlUnescape(value).trim();

        if (cleaned.isBlank()
                || "-".equals(cleaned)) {
            return null;
        }

        return cleaned;
    }

    private int parseInteger(String value) {

        if (value == null
                || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);

        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record ApiPageResult(
            String resultCode,
            String resultMessage,
            int totalCount,
            List<LoanProductApiItem> items
    ) {
    }
}
