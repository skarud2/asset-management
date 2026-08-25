package com.via.shinvia.client.card.service;

import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardQueryService {

    private final CardMapper cardMapper;

    public List<CardAccount> getCardsByConnectionId(Long connectionId) {
        return cardMapper.findAllByConnectionId(connectionId);
    }
}
