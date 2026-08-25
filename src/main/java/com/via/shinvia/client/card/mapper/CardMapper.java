package com.via.shinvia.client.card.mapper;

import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.entity.CardBill;
import com.via.shinvia.client.card.entity.CardTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CardMapper {

    Long findInstitutionIdByOrgCode(@Param("orgCode") String orgCode);

    Long findConnectionIdByUserId(@Param("userId") Long userId);

    void insertConnection(@Param("userId") Long userId);

    CardAccount findByExternalCardKey(@Param("externalCardKey") String externalCardKey);

    void insertCardAccount(CardAccount cardAccount);

    void updateCardAccount(CardAccount cardAccount);

    Long findCardAccountIdByExternalCardKey(@Param("externalCardKey") String externalCardKey);

    void upsertCardTransactions(@Param("transactions") List<CardTransaction> transactions);

    void upsertCardBills(@Param("cardBills") List<CardBill> cardBills);

    BigDecimal sumChargeAmountByUserAndMonth(@Param("userId") Long userId, @Param("chargeMonth") String chargeMonth);

    List<CardAccount> findAllByConnectionId(@Param("connectionId") Long connectionId);
}
