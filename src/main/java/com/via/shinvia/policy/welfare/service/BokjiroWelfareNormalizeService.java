package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.welfare.dto.BokjiroServiceItem;
import com.via.shinvia.welfare.dto.LocalBokjiroListResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BokjiroWelfareNormalizeService {

    public WelfareSupportProduct fromNational(
            BokjiroServiceItem item
    ) {
        return WelfareSupportProduct.builder()
                .externalId("BOKJIRO_NATIONAL:" + item.getServId())
                .productName(limit(item.getServNm(), 200))
                .institutionName(limit(firstNotBlank(
                        item.getJurMnofNm(),
                        item.getJurOrgNm()
                ), 150))
                .supportTarget(limit(item.getTrgterIndvdlArray(), 500))
                .ageCondition(limit(item.getLifeArray(), 200))
                .welfareType(limit(item.getIntrsThemaArray(), 150))
                .supportContent(item.getServDgst())
                .applicationMethod(null)
                .responsibleInstitution(limit(item.getJurOrgNm(), 200))
                .relatedUrl(limit(item.getServDtlLink(), 500))
                .sourceType("BOKJIRO_NATIONAL")
                .regionSido(null)
                .regionSigungu(null)
                .supportCycle(limit(item.getSprtCycNm(), 100))
                .supportMethod(limit(item.getSrvPvsnNm(), 100))
                .contactInfo(limit(item.getRprsCtadr(), 300))
                .onlineApplyYn(limit(item.getOnapPsbltYn(), 10))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    public WelfareSupportProduct fromLocal(
            LocalBokjiroListResponseDTO.LocalWelfareItem item
    ) {
        return WelfareSupportProduct.builder()
                .externalId("BOKJIRO_LOCAL:" + item.getServId())
                .productName(limit(item.getServNm(), 200))
                .institutionName(limit(firstNotBlank(
                        item.getJurMnofNm(),
                        item.getJurOrgNm()
                ), 150))
                .supportTarget(limit(item.getTrgterIndvdlArray(), 500))
                .ageCondition(limit(item.getLifeArray(), 200))
                .welfareType(limit(item.getIntrsThemaArray(), 150))
                .supportContent(item.getServDgst())
                .applicationMethod(null)
                .responsibleInstitution(limit(item.getJurOrgNm(), 200))
                .relatedUrl(limit(item.getServDtlLink(), 500))
                .sourceType("BOKJIRO_LOCAL")
                .regionSido(limit(item.getCtpvNm(), 50))
                .regionSigungu(limit(item.getSggNm(), 50))
                .supportCycle(limit(item.getSprtCycNm(), 100))
                .supportMethod(limit(item.getSrvPvsnNm(), 100))
                .contactInfo(limit(item.getRprsCtadr(), 300))
                .onlineApplyYn(limit(item.getOnapPsbltYn(), 10))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private String firstNotBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private String limit(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength);
    }
}
