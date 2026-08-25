package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.service.LivingExpenseEstimator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// 2단계("목표 설정")에서 보여주는 목표 프리셋 4종.
// EMERGENCY_FUND만 사용자별 월평균 생활비(LivingExpenseEstimator, 최근 3개월 카드 이용내역 기준) × 6으로 동적 계산하고,
// 나머지 3개는 고정값이다.
@Service
public class GoalPresetService {

    private static final int LIVING_EXPENSE_LOOKBACK_MONTHS = 3;
    private static final int EMERGENCY_FUND_MONTHS = 6;

    private final LivingExpenseEstimator livingExpenseEstimator;

    public GoalPresetService(LivingExpenseEstimator livingExpenseEstimator) {
        this.livingExpenseEstimator = livingExpenseEstimator;
    }

    public List<GoalPreset> getPresets(Long userId) {
        LivingExpenseEstimator.Result livingExpense =
                livingExpenseEstimator.estimate(userId, LIVING_EXPENSE_LOOKBACK_MONTHS);
        BigDecimal emergencyFundAmount =
                livingExpense.monthlyLivingExpense().multiply(BigDecimal.valueOf(EMERGENCY_FUND_MONTHS));

        return List.of(
                new GoalPreset("JEONSE", "전세보증금", new BigDecimal("200000000")),
                new GoalPreset("SEED_MONEY", "종잣돈 마련", new BigDecimal("100000000")),
                new GoalPreset("FINANCIAL_FREEDOM", "은퇴자금", new BigDecimal("1000000000")),
                new GoalPreset("EMERGENCY_FUND", "비상금 6개월치", emergencyFundAmount)
        );
    }

    public record GoalPreset(
            String key,
            String label,
            BigDecimal amount
    ) {
    }
}
