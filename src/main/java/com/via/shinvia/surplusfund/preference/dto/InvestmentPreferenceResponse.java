package com.via.shinvia.surplusfund.preference.dto;

import com.via.shinvia.surplusfund.allocation.dto.AssetAllocationResponse;
import com.via.shinvia.surplusfund.preference.model.InvestmentStyle;

import java.util.List;

public record InvestmentPreferenceResponse(
        Long surplusFundPlanId,
        InvestmentStyle investmentStyle,
        String ruleVersion,
        int score,
        List<String> reasons,
        List<AssetAllocationResponse> allocations,
        String guideNotice
) {
    public InvestmentPreferenceResponse {
        reasons = List.copyOf(reasons);
        allocations = List.copyOf(allocations);
    }
}
