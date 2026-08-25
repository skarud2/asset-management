package com.via.shinvia.account.mapper;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.model.AccountTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


// 사용할 DB 메서드를 정의하는 Mapper Interface

@Mapper
public interface AccountMapper {

    int upsertAccount(Account account);

    Long findAccountIdByExternalKey(
            @Param("connectionId") Long connectionId,
            @Param("externalAccountKey") String externalAccountKey
    );

    int insertTransactionIfAbsent(AccountTransaction transaction);

    List<Account> findAllByConnectionId(
            @Param("connectionId") Long connectionId
    );

    List<String> findOrgCodesByConnectionId(
            @Param("connectionId") Long connectionId
    );
}