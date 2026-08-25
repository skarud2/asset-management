package com.via.shinvia.account.service;

import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionItem;
import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.model.AccountTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

// Mock DTO ---> 우리 서비스 DB model로 변환
@Component
public class AccountDataConverter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Account toAccount(
            Long connectionId,
            String orgCode,
            AccountItem accountItem,
            DepositBasicItem basicItem,
            DepositDetailItem detailItem,
            LocalDateTime syncedAt
    ) {
        if (connectionId == null) {
            throw new IllegalArgumentException(
                    "connectionId가 없습니다."
            );
        }

        if (detailItem == null || detailItem.balanceAmt() == null) {
            throw new IllegalStateException(
                    "계좌 잔액 정보가 없습니다: "
                            + accountItem.accountNum()
            );
        }

        Account account = new Account();

        account.setConnectionId(connectionId);
        account.setOrgCode(orgCode);

        // externalAccountKey 만드는 로직 --> 안쓴다면 나중에 없어질 예정
        account.setExternalAccountKey(
                createExternalAccountKey(
                        orgCode,
                        accountItem.accountNum(),
                        accountItem.seqno()
                )
        );

        account.setAccountName(accountItem.prodName());

        /*
         * 지금은 팀 코드값이 확정되지 않았으므로
         * Mock에서 받은 코드값을 그대로 저장
         */
        account.setAccountStatus(accountItem.accountStatus());
        account.setAccountType(accountItem.accountType());

        account.setCurrentBalance(
                toServiceAmount(detailItem.balanceAmt())
        );

        if (basicItem != null) {
            account.setOpenedAt(
                    parseDate(basicItem.issueDate())
            );

            account.setMaturityAt(
                    parseDate(basicItem.expDate())
            );
        }

        account.setDataAsOfAt(syncedAt);

        return account;
    }

    public AccountTransaction toTransaction(
            String externalAccountKey,
            DepositTransactionItem item
    ) {
        AccountTransaction transaction =
                new AccountTransaction();

        transaction.setExternalTransactionId(
                createExternalTransactionId(
                        externalAccountKey,
                        item
                )
        );

        transaction.setTransactionAt(
                LocalDateTime.parse(
                        item.transDtime(),
                        DATE_TIME_FORMATTER
                )
        );

        /*
         * 팀의 내부 거래유형 코드가 확정되기 전까지
         * Mock trans_type 값을 그대로 저장한다.
         */
        transaction.setTransactionType(
                item.transType()
        );

        transaction.setAmount(
                toServiceAmount(item.transAmt())
        );

        transaction.setBalanceAfter(
                toServiceAmount(item.balanceAmt())
        );

        transaction.setMerchantName(null);
        transaction.setDescription(item.transMemo());

        transaction.setCategoryCode(
                hasText(item.category())
                        ? item.category()
                        : "UNCATEGORIZED"
        );

        return transaction;
    }


    // ExternalAccountKey 만드는 로직
    public String createExternalAccountKey(
            String orgCode,// 우리 동기화 API 요청 값
            String accountNum, // Mock 서버 계좌 목록 응답
            String seqno // Mock 서버 계좌 목록 응답
    ) {
        String resolvedSeqno =
                hasText(seqno) ? seqno : "NOSEQ";

        return orgCode
                + ":"
                + accountNum
                + ":"
                + resolvedSeqno;
    }

    private String createExternalTransactionId(
            String externalAccountKey,
            DepositTransactionItem item
    ) {
        if (hasText(item.transNo())) {
            return item.transNo();
        }

        String rawKey = String.join(
                "|",
                externalAccountKey,
                nullToEmpty(item.transDtime()),
                nullToEmpty(item.transType()),
                amountToString(item.transAmt()),
                amountToString(item.balanceAmt()),
                nullToEmpty(item.transMemo())
        );

        return "HASH-" + sha256(rawKey);
    }

    private BigDecimal toServiceAmount(
            BigDecimal amount
    ) {
        if (amount == null) {
            throw new IllegalStateException(
                    "금액 값이 없습니다."
            );
        }

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private LocalDate parseDate(
            String value
    ) {
        if (!hasText(value)) {
            return null;
        }

        return LocalDate.parse(
                value,
                DATE_FORMATTER
        );
    }

    private String amountToString(
            BigDecimal amount
    ) {
        if (amount == null) {
            return "";
        }

        return amount
                .stripTrailingZeros()
                .toPlainString();
    }

    private String nullToEmpty(
            String value
    ) {
        return value == null ? "" : value;
    }

    private String sha256(
            String value
    ) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = messageDigest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(bytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 생성 실패",
                    exception
            );
        }
    }

    private boolean hasText(
            String value
    ) {
        return value != null && !value.isBlank();
    }
}