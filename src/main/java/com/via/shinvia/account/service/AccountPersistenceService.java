package com.via.shinvia.account.service;

import com.via.shinvia.account.mapper.AccountMapper;
import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.model.AccountTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


// 생성된 Mapper를 호출하고 저장, 순서 트랜잭션 관리
// 처음 보는 계좌 --> 새로 저장
// 이미 있는 계좌 --> 잔액 등 최신 정보로 수정
@Service
public class AccountPersistenceService {

    private final AccountMapper accountMapper;

    public AccountPersistenceService(
            AccountMapper accountMapper
    ) {
        this.accountMapper = accountMapper;
    }

    // 트랜잭션 어노테이션으로 저장중 오류가 생기면 계좌, 계좌거래 sql 함께 rollback
    @Transactional
    public int save(
            Account account,
            List<AccountTransaction> transactions
    ) {
        accountMapper.upsertAccount(account); // Mapper XML의 upsertAccount SQL문 실행

        Long accountId =
                accountMapper.findAccountIdByExternalKey(
                        account.getConnectionId(),
                        account.getExternalAccountKey()
                );

        if (accountId == null) {
            throw new IllegalStateException(
                    "저장된 account_id를 찾지 못했습니다."
            );
        }

        // 찾는다면,
        int insertedTransactionCount = 0;

        for (AccountTransaction transaction : transactions) {
            transaction.setAccountId(accountId);

            insertedTransactionCount +=
                    accountMapper.insertTransactionIfAbsent(
                            transaction
                    );
        }

        return insertedTransactionCount;
    }
}