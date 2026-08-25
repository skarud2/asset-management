package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.dto.request.StressTestRequest;
import com.via.shinvia.stresstest.dto.response.StressTestResponse;
import com.via.shinvia.stresstest.dto.response.StressTestTimelinePoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// 대출 부담(LoanBurdenAggregator) + 지출 추정(LivingExpenseEstimator) + 유동자산/연소득
// (LiquidAssetAggregator, AnnualIncomeProvider — 둘 다 user_financial_profile 기준)을 조합해
// 생존 가능 개월수(runway)를 계산한다.
//
// user_financial_profile에 해당 사용자 행이 없으면(재무 프로필 미등록) 연소득/유동자산 중 하나라도
// 조회 불가이므로 runwayCalculable=false로 명시하고 현금흐름 계산 자체를 생략한다.
@Service
public class PersonalStressTestSimulator {

    private static final String RUNWAY_UNAVAILABLE_REASON =
            "연소득/유동자산 재무 프로필(user_financial_profile)이 아직 등록되지 않아 생존 가능 개월수를 계산할 수 없어요.";
    private static final int LOOKBACK_MONTHS = 3;
    private static final int MONTHS_IN_YEAR = 12;
    private static final int MONEY_SCALE = 2;

    private final LoanBurdenAggregator loanBurdenAggregator;
    private final LiquidAssetAggregator liquidAssetAggregator;
    private final LivingExpenseEstimator livingExpenseEstimator;
    private final AnnualIncomeProvider annualIncomeProvider;

    public PersonalStressTestSimulator(
            LoanBurdenAggregator loanBurdenAggregator,
            LiquidAssetAggregator liquidAssetAggregator,
            LivingExpenseEstimator livingExpenseEstimator,
            AnnualIncomeProvider annualIncomeProvider
    ) {
        this.loanBurdenAggregator = loanBurdenAggregator;
        this.liquidAssetAggregator = liquidAssetAggregator;
        this.livingExpenseEstimator = livingExpenseEstimator;
        this.annualIncomeProvider = annualIncomeProvider;
    }

    public StressTestResponse simulate(StressTestRequest request) {
        LoanBurdenAggregator.Result loanBurden =
                loanBurdenAggregator.aggregate(request.userId(), request.rateDeltaPercent());

        LivingExpenseEstimator.Result livingExpense =
                livingExpenseEstimator.estimate(request.userId(), LOOKBACK_MONTHS);

        LiquidAssetAggregator.Result liquidAsset =
                liquidAssetAggregator.aggregate(request.userId());

        AnnualIncomeProvider.Result income =
                annualIncomeProvider.findAnnualIncome(request.userId());

        boolean runwayCalculable = income.available() && liquidAsset.available();

        if (!runwayCalculable) {
            return new StressTestResponse(
                    false,
                    RUNWAY_UNAVAILABLE_REASON,
                    null,
                    null,
                    null,
                    null,
                    loanBurden.totalStressedLoanPayment(),
                    loanBurden.totalCurrentLoanPayment(),
                    income.available(),
                    null,
                    livingExpense.monthlyLivingExpense(),
                    livingExpense.isEstimate(),
                    livingExpense.disclaimer(),
                    liquidAsset.available(),
                    liquidAsset.totalLiquidAssets(),
                    List.of()
            );
        }

        BigDecimal monthlyIncome = income.annualIncome()
                .divide(BigDecimal.valueOf(MONTHS_IN_YEAR), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal incomeDropRatio = request.incomeDropPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal stressedIncome = monthlyIncome
                .multiply(BigDecimal.ONE.subtract(incomeDropRatio))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal netMonthlyCashflow = stressedIncome
                .subtract(loanBurden.totalStressedLoanPayment())
                .subtract(livingExpense.monthlyLivingExpense());

        BigDecimal availableAssets = clampToZero(
                liquidAsset.totalLiquidAssets().subtract(request.unexpectedExpenseAmount())
        );

        Integer runwayMonths = calculateRunwayMonths(availableAssets, netMonthlyCashflow);

        BigDecimal baselineNetMonthlyCashflow = monthlyIncome
                .subtract(loanBurden.totalCurrentLoanPayment())
                .subtract(livingExpense.monthlyLivingExpense());
        Integer baselineRunwayMonths =
                calculateRunwayMonths(liquidAsset.totalLiquidAssets(), baselineNetMonthlyCashflow);

        List<StressTestTimelinePoint> timeline =
                buildTimeline(availableAssets, netMonthlyCashflow, request.simulationMonths());

        return new StressTestResponse(
                true,
                null,
                runwayMonths,
                baselineRunwayMonths,
                netMonthlyCashflow,
                availableAssets,
                loanBurden.totalStressedLoanPayment(),
                loanBurden.totalCurrentLoanPayment(),
                true,
                stressedIncome,
                livingExpense.monthlyLivingExpense(),
                livingExpense.isEstimate(),
                livingExpense.disclaimer(),
                true,
                liquidAsset.totalLiquidAssets(),
                timeline
        );
    }

    // 현금흐름이 0 이상이면 무기한(안전)이라 null, 음수면 유동자산이 바닥나는 개월수(내림)
    private Integer calculateRunwayMonths(BigDecimal availableAssets, BigDecimal netMonthlyCashflow) {
        if (netMonthlyCashflow.signum() >= 0) {
            return null;
        }
        return availableAssets.divide(netMonthlyCashflow.abs(), 0, RoundingMode.FLOOR).intValue();
    }

    private List<StressTestTimelinePoint> buildTimeline(
            BigDecimal availableAssets, BigDecimal netMonthlyCashflow, int simulationMonths
    ) {
        List<StressTestTimelinePoint> timeline = new ArrayList<>();
        BigDecimal balance = availableAssets;

        for (int month = 0; month <= simulationMonths; month++) {
            timeline.add(new StressTestTimelinePoint(month, balance));
            balance = clampToZero(balance.add(netMonthlyCashflow));
        }

        return timeline;
    }

    private BigDecimal clampToZero(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : value;
    }
}
