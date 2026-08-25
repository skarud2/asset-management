package com.via.shinvia.finprofile;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinancialProfileMapper {
    int insertFinancialProfile(FinancialProfile financialProfile);
    int updateFinancialProfile(FinancialProfile financialProfile);
    FinancialProfile findFinancialProfileByUserId(@Param("userId") Long userId);
}
