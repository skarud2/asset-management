package com.via.shinvia.surplusfund.calculation.mapper;

import com.via.shinvia.surplusfund.calculation.entity.SurplusFundCalculation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface SurplusFundMapper {

    BigDecimal sumAccountLivingExpense(
            @Param("connectionId") Long connectionId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    BigDecimal sumCardLivingExpense(
            @Param("connectionId") Long connectionId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    LocalDateTime findLatestSalaryIncomeDate(
            @Param("connectionId") Long connectionId
    );

    void insertCalculation(SurplusFundCalculation calculation);

    SurplusFundCalculation findLatestCalculationByUserId(
            @Param("userId") Long userId
    );
}