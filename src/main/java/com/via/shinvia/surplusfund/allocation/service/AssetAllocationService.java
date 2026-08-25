package com.via.shinvia.surplusfund.allocation.service;


// 운용 성향에 따른 CASH/ETF/FUND 비율 계산 서비스단

import com.via.shinvia.surplusfund.allocation.dto.AssetAllocationResponse;
import com.via.shinvia.surplusfund.allocation.model.AssetType;

import com.via.shinvia.surplusfund.preference.model.InvestmentStyle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AssetAllocationService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private static final Map<InvestmentStyle, Map <AssetType, BigDecimal>> ratios = Map.of (
            InvestmentStyle.STABLE, Map.of(
                    AssetType.CASH, new BigDecimal("60.00"),
                    AssetType.ETF, new BigDecimal("20.00"),
                    AssetType.FUND, new BigDecimal("20.00")
            ),
            InvestmentStyle.BALANCED, Map.of(
                    AssetType.CASH, new BigDecimal("30.00"),
                    AssetType.ETF, new BigDecimal("50.00"),
                    AssetType.FUND, new BigDecimal("20.00")
            ),
            InvestmentStyle.AGGRESSIVE, Map.of(
                    AssetType.CASH, new BigDecimal("10.00"),
                    AssetType.ETF, new BigDecimal("65.00"),
                    AssetType.FUND, new BigDecimal("25.00")
            )
    );


    public List<AssetAllocationResponse>  allocate  (
            BigDecimal operationAmount, InvestmentStyle investmentStyle
    ) {
        if (operationAmount == null || operationAmount.signum() <=0) {
            throw new IllegalArgumentException("운용금액은 0보다 커야합니다.");
        }
        if (investmentStyle ==null) {
            throw new IllegalArgumentException("투자 성향은 필수");
        }


        BigDecimal normalizedAmount = operationAmount.setScale(2, RoundingMode.HALF_UP);
        Map <AssetType, BigDecimal> styleRatios = ratios.get(investmentStyle);
        validateRatioSum(styleRatios);


        List<AssetAllocationResponse> allocations = new ArrayList<>();
        BigDecimal remainingAmount = normalizedAmount;
        AssetType [] assetTypes = AssetType.values();

        for (int index=  0; index <assetTypes.length; index ++) {
            AssetType assetType = assetTypes[index];
            BigDecimal ratio = styleRatios.get(assetType);

            BigDecimal allocationAmount;
            if (index == assetTypes.length -1) {
                allocationAmount = remainingAmount;
            }else {
                allocationAmount = normalizedAmount.multiply(ratio).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
                remainingAmount = remainingAmount.subtract(allocationAmount);
            }

            allocations.add (new AssetAllocationResponse(assetType, ratio, allocationAmount));
        }
        return allocations;

    }
    private void validateRatioSum(Map<AssetType,BigDecimal> ratios) {
        BigDecimal ratioSum = ratios.values().stream().reduce (BigDecimal.ZERO, BigDecimal::add);
        if (ratioSum.compareTo(ONE_HUNDRED) !=0) {
            throw new IllegalStateException("자산배분 비율 합계가 100%가 아닙니다.");

        }

        for (AssetType assetType : AssetType.values()) {
            if (!ratios.containsKey(assetType)) {
                throw new IllegalStateException("자산배분 비율에 " + assetType + "이 누락됐습니다.");
            }
        }
    }


}


