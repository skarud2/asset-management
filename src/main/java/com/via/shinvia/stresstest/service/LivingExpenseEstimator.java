package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestCardTransactionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// 최근 lookbackMonths개월간의 카드 이용금액 평균으로 월평균 지출을 추정한다.
// account_transaction(계좌 출금내역)은 테이블이 없어 반영하지 못하고 카드 이용내역만 반영한다.
// card_transaction에 카테고리/거래유형 구분이 없어(설계 당시 마이데이터 API에 카테고리가 없어 제외) 필수/선택
// 지출을 나누지 못하는 전체 지출 평균이라는 한계가 있다.
@Service
public class LivingExpenseEstimator {

    private static final String DISCLAIMER =
            "카드 거래내역만 반영한 추정치이며(계좌 출금내역 미포함), 필수/선택 지출을 구분하지 않은 전체 지출 평균입니다.";
    private static final int MONEY_SCALE = 2;

    private final StressTestCardTransactionMapper cardTransactionMapper;

    public LivingExpenseEstimator(StressTestCardTransactionMapper cardTransactionMapper) {
        this.cardTransactionMapper = cardTransactionMapper;
    }

    public Result estimate(Long userId, int lookbackMonths) {
        if (lookbackMonths <= 0) {
            throw new IllegalArgumentException("lookbackMonths는 1 이상이어야 해요");
        }

        LocalDateTime since = LocalDateTime.now().minusMonths(lookbackMonths);
        BigDecimal totalCardSpend = cardTransactionMapper.sumAmountByUserIdSince(userId, since);

        BigDecimal monthlyLivingExpense = totalCardSpend
                .divide(BigDecimal.valueOf(lookbackMonths), MONEY_SCALE, RoundingMode.HALF_UP);

        return new Result(monthlyLivingExpense, true, DISCLAIMER);
    }

    public record Result(
            BigDecimal monthlyLivingExpense,
            boolean isEstimate,
            String disclaimer
    ) {
    }
}
