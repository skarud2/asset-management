package com.via.shinvia.client.card.controller.dto;

import com.via.shinvia.client.card.billdetail.response.CardBillDetailDto;
import com.via.shinvia.client.card.entity.CardTransaction;

import java.util.List;

/**
 * 목서버에서 받은 원본 청구상세 목록(received)과 그중 card_transaction에 실제로 저장된 5개 필드(saved)를 함께 보여준다.
 * received에는 있지만 saved에는 없는 필드(total_install_cnt, cur_install_cnt, balance_amt 등)는 저장하지 않기로 한 값이다.
 */
public record CardTransactionSyncResponse(List<CardBillDetailDto> received, List<CardTransaction> saved) {
}
