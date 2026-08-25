package com.via.shinvia.report.service;

import com.via.shinvia.report.dto.ReportCardSelection;
import com.via.shinvia.report.dto.response.ReportCardOptionResponse;
import com.via.shinvia.report.entity.ReportCardLayout;
import com.via.shinvia.report.mapper.ReportCardLayoutMapper;
import com.via.shinvia.report.service.provider.ReportCardDataProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportCardService {

    private final Map<String, ReportCardDataProvider> providers;
    private final ReportCardLayoutMapper layoutMapper;

    public ReportCardService(List<ReportCardDataProvider> providerList, ReportCardLayoutMapper layoutMapper) {
        Map<String, ReportCardDataProvider> registry = new LinkedHashMap<>();
        for (ReportCardDataProvider provider : providerList) {
            registry.put(provider.getCardKey(), provider);
        }
        this.providers = registry;
        this.layoutMapper = layoutMapper;
    }

    public List<ReportCardOptionResponse> listOptions(Long userId) {
        return providers.values().stream()
                .map(p -> new ReportCardOptionResponse(p.getCardKey(), p.getCardData(userId, null).title(), p.isAvailable()))
                .toList();
    }

    public ReportCardDataProvider.CardData getCardData(String cardKey, Long userId, Long refId) {
        ReportCardDataProvider provider = requireProvider(cardKey);
        if (!provider.isAvailable()) {
            throw new IllegalArgumentException("아직 사용할 수 없는 카드예요: " + cardKey);
        }
        return provider.getCardData(userId, refId);
    }

    public List<ReportCardSelection> getLayout(Long userId) {
        return layoutMapper.findAllByUserIdOrderByDisplayOrder(userId).stream()
                .map(row -> new ReportCardSelection(normalizeCardKey(row.getCardKey()), row.getRefId()))
                .toList();
    }

    public void saveLayout(Long userId, List<ReportCardSelection> selections) {
        for (ReportCardSelection selection : selections) {
            ReportCardDataProvider provider = requireProvider(selection.cardKey());
            if (!provider.isAvailable()) {
                throw new IllegalArgumentException("아직 사용할 수 없는 카드예요: " + selection.cardKey());
            }
        }
        layoutMapper.deleteByUserId(userId);
        int order = 0;
        for (ReportCardSelection selection : selections) {
            layoutMapper.insert(userId, normalizeCardKey(selection.cardKey()), selection.refId(), order++);
        }
    }

    private ReportCardDataProvider requireProvider(String cardKey) {
        ReportCardDataProvider provider = providers.get(normalizeCardKey(cardKey));
        if (provider == null) {
            throw new IllegalArgumentException("존재하지 않는 카드 종류예요: " + cardKey);
        }
        return provider;
    }

    private String normalizeCardKey(String cardKey) {
        return "lifecycle_scenario".equals(cardKey) ? "FINANCIAL_CYCLE_PLAN" : cardKey;
    }
}
