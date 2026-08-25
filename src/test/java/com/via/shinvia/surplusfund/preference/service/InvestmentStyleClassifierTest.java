package com.via.shinvia.surplusfund.preference.service;

import com.via.shinvia.surplusfund.allocation.dto.AssetAllocationResponse;
import com.via.shinvia.surplusfund.allocation.model.AssetType;
import com.via.shinvia.surplusfund.allocation.service.AssetAllocationService;
import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceRequest;
import com.via.shinvia.surplusfund.preference.model.ExperienceLevel;
import com.via.shinvia.surplusfund.preference.model.InvestmentPurpose;
import com.via.shinvia.surplusfund.preference.model.InvestmentStyle;
import com.via.shinvia.surplusfund.preference.model.LiquidityNeed;
import com.via.shinvia.surplusfund.preference.model.LossToleranceLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvestmentStyleClassifierTest {

    private final InvestmentStyleClassifier classifier =
            new InvestmentStyleClassifier();

    private final AssetAllocationService allocationService =
            new AssetAllocationService();

    @Test
    void balancedStyleAndAllocationTest() {
        // given: 균형성장을 목적으로 하는 사용자 설문
        InvestmentPreferenceRequest request =
                new InvestmentPreferenceRequest(
                        new BigDecimal("1500000"),
                        InvestmentPurpose.BALANCED_GROWTH,
                        36,
                        LossToleranceLevel.WITHIN_15_PERCENT,
                        LiquidityNeed.MEDIUM,
                        ExperienceLevel.LIMITED,
                        true,
                        true
                );

        // when: 투자성향 판정
        InvestmentStyleClassifier.ClassificationResult result =
                classifier.classify(request);

        // then: 총점 8점, 균형형
        assertEquals(InvestmentStyle.BALANCED, result.investmentStyle());
        assertEquals(8, result.score());

        // when: 자산배분 계산
        List<AssetAllocationResponse> allocations =
                allocationService.allocate(
                        request.operationAmount(),
                        result.investmentStyle()
                );

        // then: CASH 30%, ETF 50%, FUND 20%
        assertEquals(3, allocations.size());

        assertAllocation(
                allocations,
                AssetType.CASH,
                "30.00",
                "450000.00"
        );

        assertAllocation(
                allocations,
                AssetType.ETF,
                "50.00",
                "750000.00"
        );

        assertAllocation(
                allocations,
                AssetType.FUND,
                "20.00",
                "300000.00"
        );
    }

    private void assertAllocation(
            List<AssetAllocationResponse> allocations,
            AssetType assetType,
            String expectedRatio,
            String expectedAmount
    ) {
        AssetAllocationResponse allocation = allocations.stream()
                .filter(item -> item.assetType() == assetType)
                .findFirst()
                .orElseThrow();

        assertEquals(
                0,
                allocation.ratio().compareTo(
                        new BigDecimal(expectedRatio)
                )
        );

        assertEquals(
                0,
                allocation.amount().compareTo(
                        new BigDecimal(expectedAmount)
                )
        );
    }
}