package com.via.shinvia.loananalysis.mapper;

import com.via.shinvia.loananalysis.dto.FinancialCapacityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 재무여력 조회 Mapper
@Mapper
public interface FinancialCapacityMapper {

    // 사용자 최신 재무정보 조회
    FinancialCapacityDTO findFinancialCapacityByUserId(
            @Param("userId") Long userId
    );
}