package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestFinancialProfileMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// account/account_transaction(실제 계좌 잔액) 테이블은 여전히 존재하지 않아서(db/init/001_schema.sql 참고),
// 대신 user_financial_profile.liquid_asset_amount를 유동자산으로 쓴다. 이건 실시간 계좌 잔액 합산이 아니라
// 사용자가 별도로 등록/관리하는 재무 프로필 값이라는 차이가 있다. 프로필이 없는 사용자는 available=false.
@Service
public class LiquidAssetAggregator {

    private final StressTestFinancialProfileMapper financialProfileMapper;

    public LiquidAssetAggregator(StressTestFinancialProfileMapper financialProfileMapper) {
        this.financialProfileMapper = financialProfileMapper;
    }

    public Result aggregate(Long userId) {
        BigDecimal liquidAssetAmount = financialProfileMapper.findLiquidAssetAmountByUserId(userId);

        if (liquidAssetAmount == null) {
            return new Result(false, null);
        }

        return new Result(true, liquidAssetAmount);
    }

    public record Result(
            boolean available,
            BigDecimal totalLiquidAssets
    ) {
    }
}
