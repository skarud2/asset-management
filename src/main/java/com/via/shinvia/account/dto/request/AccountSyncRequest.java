package com.via.shinvia.account.dto.request;

public record AccountSyncRequest(
        String orgCode,
        String fromDate,
        String toDate,
        Integer limit
) {

    public int resolvedLimit() {
        return limit == null ? 500 : limit;
    }
}