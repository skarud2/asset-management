package com.via.shinvia.stresstest.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface StressTestFinancialProfileMapper {

    BigDecimal findAnnualIncomeByUserId(@Param("userId") Long userId);

    BigDecimal findLiquidAssetAmountByUserId(@Param("userId") Long userId);
}
