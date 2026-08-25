package com.via.shinvia.mydata.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FinancialInstitutionMapper {
    List<String> findBankOrgCodes();
}
