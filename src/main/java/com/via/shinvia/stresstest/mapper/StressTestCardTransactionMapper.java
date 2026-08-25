package com.via.shinvia.stresstest.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface StressTestCardTransactionMapper {

    BigDecimal sumAmountByUserIdSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}
