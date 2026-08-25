package com.via.shinvia.marketdata.mapper;

import com.via.shinvia.marketdata.entity.BondYieldCurveRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BondYieldCurveMapper {

    List<BondYieldCurveRow> findByAsOfDate(@Param("asOfDate") LocalDate asOfDate);
}
