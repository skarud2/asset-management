package com.via.shinvia.loan.ratesimulation.marketimplied.service;

import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.loan.ratesimulation.marketimplied.dto.response.MarketImpliedRateStep;
import com.via.shinvia.loan.ratesimulation.marketimplied.dto.response.MarketImpliedSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import com.via.shinvia.marketdata.ForwardRateCurveService;
import com.via.shinvia.marketdata.ForwardRatePoint;
import com.via.shinvia.marketdata.NoYieldCurveDataException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 채권 수익률곡선의 내재 선도금리를 대출에 적용해 시장금리 기대 경로대로면 월상환액이 어떻게
// 변할지 계산. 선도금리 계산은 marketdata.ForwardRateCurveService, 대출 시뮬레이션은
// StagedRateSimulator.simulateCustomPath (6-4에서 만든 엔진 재사용, 무변경)에 위임한다.
@Service
public class MarketImpliedRateSimulator {

    private static final String NORMAL_LOAN_STATUS = "정상";
    private static final String FIXED_RATE_TYPE = "고정";
    private static final String DATA_SOURCE = "내부 참고용 수익률곡선 (KOFIA 채권정보센터 승인 전 — 수동 입력값 기반)";

    private final LoanAccountSummaryMapper loanAccountSummaryMapper;
    private final LoanRepaymentCalculator repaymentCalculator;
    private final StagedRateSimulator stagedRateSimulator;
    private final ForwardRateCurveService forwardRateCurveService;

    public MarketImpliedRateSimulator(
            LoanAccountSummaryMapper loanAccountSummaryMapper,
            LoanRepaymentCalculator repaymentCalculator,
            StagedRateSimulator stagedRateSimulator,
            ForwardRateCurveService forwardRateCurveService
    ) {
        this.loanAccountSummaryMapper = loanAccountSummaryMapper;
        this.repaymentCalculator = repaymentCalculator;
        this.stagedRateSimulator = stagedRateSimulator;
        this.forwardRateCurveService = forwardRateCurveService;
    }

    public MarketImpliedSimulationResponse simulate(Long loanId) {
        LoanAccountSummary loan = loanAccountSummaryMapper.findById(loanId);
        if (loan == null) {
            throw new LoanNotFoundException(loanId);
        }
        if (!NORMAL_LOAN_STATUS.equals(loan.getLoanStatus())) {
            throw new InvalidLoanStatusException();
        }

        LocalDate asOfDate = LocalDate.now();

        if (FIXED_RATE_TYPE.equals(loan.getRateType())) {
            return new MarketImpliedSimulationResponse(
                    loan.getLoanAccountId(),
                    loan.getLoanType(),
                    loan.getRateType(),
                    asOfDate,
                    loan.getInterestRate(),
                    List.of(),
                    false,
                    "고정금리 대출이라 시장 금리 변동의 영향을 받지 않아요",
                    DATA_SOURCE
            );
        }

        List<ForwardRatePoint> forwardRates = forwardRateCurveService.calculateForwardRates(asOfDate);
        if (forwardRates.isEmpty()) {
            throw new NoYieldCurveDataException(asOfDate);
        }

        // 대출의 재산정 시점 개념을 별도로 두지 않고, 수익률곡선의 만기(tenor) 지점을 그대로 재산정 시점으로 사용
        List<CustomRatePathPoint> ratePath = new ArrayList<>();
        ratePath.add(new CustomRatePathPoint(0, loan.getInterestRate()));
        for (ForwardRatePoint forwardRate : forwardRates) {
            ratePath.add(new CustomRatePathPoint(forwardRate.monthOffset(), forwardRate.impliedRate()));
        }

        int remainingMonths0 = repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt());

        StagedRateSimulator.CustomPathSimulationResult result = stagedRateSimulator.simulateCustomPath(
                loan.getCurrentBalance(), remainingMonths0, loan.getRepaymentType(), ratePath
        );

        List<MarketImpliedRateStep> path = result.path().stream()
                .filter(step -> step.monthOffset() > 0)
                .map(step -> new MarketImpliedRateStep(step.monthOffset(), step.appliedRate(), step.monthlyPayment()))
                .toList();

        return new MarketImpliedSimulationResponse(
                loan.getLoanAccountId(),
                loan.getLoanType(),
                loan.getRateType(),
                asOfDate,
                loan.getInterestRate(),
                path,
                result.truncated(),
                null,
                DATA_SOURCE
        );
    }
}
