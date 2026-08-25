package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.dto.response.RateRiskReferenceResponse;
import com.via.shinvia.futuresim.event.RateChangeEvent;
import com.via.shinvia.futuresim.event.RateChangeEventResolver;
import com.via.shinvia.futuresim.event.RateChangeMode;
import com.via.shinvia.loan.ratesimulation.breakeven.service.BreakevenRateCalculator;
import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 4단계 [섹션 2] "금리 리스크 참고 카드" — 사용자의 대표 대출(LeverIntensityCalculator와 동일 기준,
// 잔액이 가장 큰 대출)에 금리 변화 시나리오(SIMPLE/STAGED/MARKET_IMPLIED)를 적용해서 상환 경로를 보여준다.
// 실제 상환 스케줄 계산은 loan/ratesimulation의 StagedRateSimulator.simulateCustomPath()에 위임하고
// (읽기 전용 재사용, 수정 없음), 여기서는 "레버 랭킹" 화면에 맞는 입출력만 조립한다.
@Service
public class RateRiskReferenceService {

    private final LeverIntensityCalculator leverIntensityCalculator;
    private final StagedRateSimulator stagedRateSimulator;
    private final RateChangeEventResolver rateChangeEventResolver;
    private final BreakevenRateCalculator breakevenRateCalculator;
    private final LoanRepaymentCalculator repaymentCalculator;
    private final AppConfigService appConfigService;

    private static final String THRESHOLD_MULTIPLIER_CONFIG_KEY = "FUTURESIM_RATE_RISK_THRESHOLD_MULTIPLIER";

    public RateRiskReferenceService(
            LeverIntensityCalculator leverIntensityCalculator,
            StagedRateSimulator stagedRateSimulator,
            RateChangeEventResolver rateChangeEventResolver,
            BreakevenRateCalculator breakevenRateCalculator,
            LoanRepaymentCalculator repaymentCalculator,
            AppConfigService appConfigService
    ) {
        this.leverIntensityCalculator = leverIntensityCalculator;
        this.stagedRateSimulator = stagedRateSimulator;
        this.rateChangeEventResolver = rateChangeEventResolver;
        this.breakevenRateCalculator = breakevenRateCalculator;
        this.repaymentCalculator = repaymentCalculator;
        this.appConfigService = appConfigService;
    }

    public RateRiskReferenceResponse calculate(Long userId, RateChangeMode mode) {
        StressTestLoanRow loan = leverIntensityCalculator.representativeLoan(userId);
        if (loan == null) {
            return RateRiskReferenceResponse.unavailable(mode);
        }

        int remainingMonths = Math.max(1, repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt()));
        RateChangeEvent event = RateChangeEvent.of(mode);
        List<CustomRatePathPoint> ratePath = rateChangeEventResolver.resolve(event, loan.getInterestRate());

        StagedRateSimulator.CustomPathSimulationResult simulation = stagedRateSimulator.simulateCustomPath(
                loan.getCurrentBalance(), remainingMonths, loan.getRepaymentType(), ratePath
        );

        BigDecimal thresholdMultiplier = appConfigService.getDecimal(THRESHOLD_MULTIPLIER_CONFIG_KEY);
        BigDecimal thresholdMonthlyPayment = simulation.initialMonthlyPayment()
                .multiply(thresholdMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        BreakevenRateCalculator.Result breakeven = breakevenRateCalculator.search(
                loan.getCurrentBalance(), loan.getInterestRate(), remainingMonths,
                loan.getRepaymentType(), thresholdMonthlyPayment
        );

        return new RateRiskReferenceResponse(
                true,
                mode,
                loan.getLoanType(),
                loan.getCurrentBalance(),
                loan.getInterestRate(),
                simulation.initialMonthlyPayment(),
                simulation.path(),
                breakeven.breakevenRate(),
                breakeven.reached(),
                breakeven.alreadyExceeded(),
                thresholdMonthlyPayment
        );
    }
}
