package com.via.shinvia.account.dto.response;

public record AccountSyncResult(
        int discoveredAccounts,
        int syncedAccounts,
        int skippedAccounts,
        int insertedTransactions
) {
}