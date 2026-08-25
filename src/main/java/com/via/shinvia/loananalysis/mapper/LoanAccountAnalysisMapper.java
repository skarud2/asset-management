package com.via.shinvia.loananalysis.mapper;

import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 대출계좌 분석 Mapper
@Mapper
public interface LoanAccountAnalysisMapper {

    // 사용자 보유 대출 전체 조회
    List<LoanAccountAnalysisDTO> findActiveLoansByUserId(
            @Param("userId") Long userId
    );

    // 사용자 대출 1건 조회
    LoanAccountAnalysisDTO findLoanById(
            @Param("userId") Long userId,
            @Param("loanAccountId") Long loanAccountId
    );

}