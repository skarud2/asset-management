package com.via.shinvia.report.service.provider;

import com.via.shinvia.report.service.provider.ReportCardDataProvider.CardData.DetailRow;
import com.via.shinvia.stresstest.service.LiquidAssetAggregator;
import com.via.shinvia.stresstest.service.LoanBurdenAggregator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class AssetDebtCardProvider implements ReportCardDataProvider {

    private static final String CARD_KEY = "ASSET_DEBT";
    private static final BigDecimal DEFAULT_RATE_DELTA_PERCENT = new BigDecimal("1.0");

    private final LiquidAssetAggregator liquidAssetAggregator;
    private final LoanBurdenAggregator loanBurdenAggregator;

    public AssetDebtCardProvider(LiquidAssetAggregator liquidAssetAggregator, LoanBurdenAggregator loanBurdenAggregator) {
        this.liquidAssetAggregator = liquidAssetAggregator;
        this.loanBurdenAggregator = loanBurdenAggregator;
    }

    @Override
    public String getCardKey() {
        return CARD_KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CardData getCardData(Long userId, Long refId) {
        LiquidAssetAggregator.Result liquidAsset = liquidAssetAggregator.aggregate(userId);
        LoanBurdenAggregator.Result loanBurden = loanBurdenAggregator.aggregate(userId, DEFAULT_RATE_DELTA_PERCENT);

        String headlineValue = liquidAsset.available() ? formatWon(liquidAsset.totalLiquidAssets()) : "-";
        BigDecimal increase = loanBurden.totalStressedLoanPayment().subtract(loanBurden.totalCurrentLoanPayment());

        List<DetailRow> detailRows = List.of(
                new DetailRow("현재 월 상환액", formatWon(loanBurden.totalCurrentLoanPayment())),
                new DetailRow("금리 +1.0%p 시 월 상환액", formatWon(loanBurden.totalStressedLoanPayment())),
                new DetailRow("금리 상승分", (increase.signum() > 0 ? "+" : "") + formatWon(increase))
        );

        String note = liquidAsset.available()
                ? null
                : "유동자산 정보가 아직 등록되지 않았어요.";

        return new CardData(CARD_KEY, "자산·부채 현황", "보유 유동자산", headlineValue, detailRows, note, null,null);
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) return "-";
        return String.format(Locale.KOREA, "%,d원", amount.longValue());
    }
}
