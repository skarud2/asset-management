package com.via.shinvia.loan.catalog.service;

import com.via.shinvia.loan.catalog.exception.LoanProductCatalogNotFoundException;
import com.via.shinvia.loan.catalog.mapper.LoanProductCatalogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanProductCatalogQueryService {

    private final LoanProductCatalogMapper mapper;

    public List<Map<String, Object>> list(String loanType, Boolean active) {
        String normalized = loanType == null || loanType.isBlank()
                ? null
                : loanType.trim().toUpperCase();
        return mapper.findCatalogSummaries(normalized, active);
    }

    public Map<String, Object> detail(Long catalogProductId) {
        Map<String, Object> summary = mapper.findCatalogSummaryById(catalogProductId);
        if (summary == null) {
            throw new LoanProductCatalogNotFoundException(catalogProductId);
        }

        Map<String, Object> result = new LinkedHashMap<>(summary);
        String loanType = String.valueOf(summary.get("loanType"));

        if ("MORTGAGE".equals(loanType) || "JEONSE".equals(loanType)) {
            result.put("detail", mapper.findHousingDetail(catalogProductId));
            result.put("options", mapper.findHousingOptions(catalogProductId));
        } else if ("CREDIT".equals(loanType)) {
            result.put("detail", mapper.findCreditDetail(catalogProductId));
            result.put("options", mapper.findCreditOptions(catalogProductId));
        } else {
            result.put("detail", null);
            result.put("options", List.of());
        }
        return result;
    }
}
