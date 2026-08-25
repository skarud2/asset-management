package com.via.shinvia.futuresim.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/** FutureSim의 시작 유동자산 계산에 필요한 사용자 금융 데이터를 조회한다. */
@Mapper
public interface FutureSimFinancialSnapshotMapper {

    BigDecimal findLiquidAssetAmountByUserId(@Param("userId") Long userId);

    BigDecimal sumAccountBalanceByUserId(@Param("userId") Long userId);
}
