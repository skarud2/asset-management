package com.via.shinvia.account.service;

import com.via.shinvia.account.mapper.AccountMapper;
import com.via.shinvia.account.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private final AccountMapper accountMapper;

    public List<Account> getAccountsByConnectionId(Long connectionId) {
        return accountMapper.findAllByConnectionId(connectionId);
    }

    public List<String> getOrgCodesByConnectionId(Long connectionId) {
        return accountMapper.findOrgCodesByConnectionId(
                connectionId
        );
    }
}