package com.via.shinvia.surplusfund.product.fund.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class FundProductCodeGenerator {
    private FundProductCodeGenerator() {
    }

    public static String generate(
            String providerName,
            String productName,
            String fundType,
            Integer fundGrade,
            BigDecimal upfrontFeeRate,
            BigDecimal totalExpenseRate
    ) {

        String identityKey = String.join("|",
                normalize(providerName),
                normalize(productName),
                normalize(fundType),
                fundGrade == null ? "" : fundGrade.toString(),
                normalize(upfrontFeeRate),
                normalize(totalExpenseRate)
        );

        UUID uuid = UUID.nameUUIDFromBytes(identityKey.getBytes(StandardCharsets.UTF_8));

        return "IBK_" + uuid;
    }

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalize(BigDecimal value) {

        if (value == null) {
            return "";
        }

        return value.stripTrailingZeros().toPlainString();
    }
}
