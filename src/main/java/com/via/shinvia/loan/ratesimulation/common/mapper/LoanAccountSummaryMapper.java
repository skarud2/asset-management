package com.via.shinvia.loan.ratesimulation.common.mapper;

import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 한계금리 역산 기능 전용 최소 조회 Mapper (팀원의 loan 도메인 Mapper와 분리)
@Mapper
public interface LoanAccountSummaryMapper {

    LoanAccountSummary findById(@Param("loanAccountId") Long loanAccountId);
}
