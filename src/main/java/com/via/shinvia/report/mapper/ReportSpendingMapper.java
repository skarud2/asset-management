package com.via.shinvia.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReportSpendingMapper {

    List<BigDecimal> findMonthlyTotalsByUserId(@Param("userId") Long userId, @Param("monthsBack") int monthsBack);
}
