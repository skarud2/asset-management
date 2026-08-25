package com.via.shinvia.loan.account.mapper;

import com.via.shinvia.loan.account.entity.LoanAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoanAccountMapper {

    LoanAccount findByExternalLoanKey(
            @Param("connectionId") Long connectionId,
            @Param("externalLoanKey") String externalLoanKey
    );

    List<LoanAccount> findAllByConnectionId(@Param("connectionId") Long connectionId);

    int insertLoanAccount(LoanAccount loanAccount);

    int updateLoanAccount(LoanAccount loanAccount);
}