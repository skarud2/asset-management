package com.via.shinvia.loan.ratesimulation.common.service;

import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.loan.ratesimulation.staged.dto.request.StagedRateSimulationRequest;
import com.via.shinvia.loan.ratesimulation.staged.dto.response.StagedRateSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.dto.response.StagedRateStep;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// 재산정 주기마다 금리가 계단식으로 변한다고 가정했을 때, 시점별 월상환액/잔여원금 변화를 계산
@Service
public class StagedRateSimulator {

    private static final String NORMAL_LOAN_STATUS = "정상";
    private static final String FIXED_RATE_TYPE = "고정";

    private final LoanAccountSummaryMapper loanAccountSummaryMapper;
    private final LoanRepaymentCalculator repaymentCalculator;

    public StagedRateSimulator(
            LoanAccountSummaryMapper loanAccountSummaryMapper,
            LoanRepaymentCalculator repaymentCalculator
    ) {
        this.loanAccountSummaryMapper = loanAccountSummaryMapper;
        this.repaymentCalculator = repaymentCalculator;
    }

    public StagedRateSimulationResponse simulate(StagedRateSimulationRequest request) {
        if (request.repricingCycleMonths() <= 0) {
            throw new IllegalArgumentException("repricingCycleMonths는 1 이상이어야 해요");
        }
        if (request.stepCount() <= 0) {
            throw new IllegalArgumentException("stepCount는 1 이상이어야 해요");
        }

        LoanAccountSummary loan = loanAccountSummaryMapper.findById(request.loanId());
        if (loan == null) {
            throw new LoanNotFoundException(request.loanId());
        }
        if (!NORMAL_LOAN_STATUS.equals(loan.getLoanStatus())) {
            throw new InvalidLoanStatusException();
        }

        if (FIXED_RATE_TYPE.equals(loan.getRateType())) {
            return new StagedRateSimulationResponse(
                    loan.getLoanAccountId(),
                    loan.getLoanType(),
                    loan.getRateType(),
                    request.repricingCycleMonths(),
                    request.stepDeltaPercent(),
                    null,
                    null,
                    List.of(),
                    null,
                    false,
                    "고정금리 대출이라 재산정에 따른 금리 변동이 없어요"
            );
        }

        int remainingMonths0 = repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt());
        String repaymentType = loan.getRepaymentType();

        BigDecimal balance = loan.getCurrentBalance();
        BigDecimal rate = loan.getInterestRate();
        int elapsedTotal = 0;

        BigDecimal initialMonthlyPayment =
                repaymentCalculator.calculate(balance, rate, remainingMonths0, repaymentType).monthlyPayment();

        List<StagedRateStep> path = new ArrayList<>();
        path.add(new StagedRateStep(0, rate, balance, initialMonthlyPayment, null));

        BigDecimal previousMonthlyPayment = initialMonthlyPayment;
        boolean truncated = false;
        BigDecimal totalSegmentInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (int i = 1; i <= request.stepCount(); i++) {
            if (elapsedTotal + request.repricingCycleMonths() >= remainingMonths0) {
                truncated = true;
                break;
            }

            int totalMonthsForSegment = remainingMonths0 - elapsedTotal;
            BigDecimal newBalance = repaymentCalculator.calculateRemainingBalance(
                    balance, rate, totalMonthsForSegment, request.repricingCycleMonths(), repaymentType
            );

            BigDecimal segmentInterest = previousMonthlyPayment
                    .multiply(BigDecimal.valueOf(request.repricingCycleMonths()))
                    .subtract(balance.subtract(newBalance));
            totalSegmentInterest = totalSegmentInterest.add(segmentInterest);

            elapsedTotal += request.repricingCycleMonths();
            rate = rate.add(request.stepDeltaPercent());
            balance = newBalance;

            int newRemainingMonths = remainingMonths0 - elapsedTotal;
            BigDecimal monthlyPayment =
                    repaymentCalculator.calculate(balance, rate, newRemainingMonths, repaymentType).monthlyPayment();

            path.add(new StagedRateStep(elapsedTotal, rate, balance, monthlyPayment, segmentInterest));
            previousMonthlyPayment = monthlyPayment;
        }

        return new StagedRateSimulationResponse(
                loan.getLoanAccountId(),
                loan.getLoanType(),
                loan.getRateType(),
                request.repricingCycleMonths(),
                request.stepDeltaPercent(),
                loan.getInterestRate(),
                initialMonthlyPayment,
                path,
                totalSegmentInterest,
                truncated,
                null
        );
    }

    // 재산정 주기가 고정 간격이 아니라, 외부에서 주어진 임의의 (monthOffset, rate) 시퀀스를 그대로 재생
    // (과거 금리인상기 재현 등, 실제 변경 이력처럼 간격이 불규칙한 시나리오에서 사용)
    // ratePath는 monthOffset 오름차순으로 정렬되어 있어야 하고, 첫 원소는 monthOffset=0이어야 함
    public CustomPathSimulationResult simulateCustomPath(
            BigDecimal initialBalance,
            int remainingMonths0,
            String repaymentType,
            List<CustomRatePathPoint> ratePath
    ) {
        if (ratePath.isEmpty()) {
            throw new IllegalArgumentException("ratePath는 최소 1개 이상이어야 해요");
        }

        BigDecimal balance = initialBalance;
        BigDecimal rate = ratePath.get(0).rate();
        int elapsedTotal = 0;

        BigDecimal initialMonthlyPayment =
                repaymentCalculator.calculate(balance, rate, remainingMonths0, repaymentType).monthlyPayment();

        List<StagedRateStep> path = new ArrayList<>();
        path.add(new StagedRateStep(0, rate, balance, initialMonthlyPayment, null));

        BigDecimal previousMonthlyPayment = initialMonthlyPayment;
        BigDecimal totalSegmentInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean truncated = false;

        for (int i = 1; i < ratePath.size(); i++) {
            CustomRatePathPoint point = ratePath.get(i);
            int segmentMonths = point.monthOffset() - elapsedTotal;

            if (segmentMonths <= 0) {
                // 같은 개월수 구간 안에서 또 바뀐 경우 (예: 월 단위로 반올림하면 두 변경일이 같은 달에 몰림)
                // -> 별도 스텝을 만들지 않고, 다음 구간에 적용할 금리/월상환액만 최신값으로 갱신
                rate = point.rate();
                int remainingAtElapsed = remainingMonths0 - elapsedTotal;
                previousMonthlyPayment =
                        repaymentCalculator.calculate(balance, rate, remainingAtElapsed, repaymentType).monthlyPayment();
                continue;
            }
            if (elapsedTotal + segmentMonths >= remainingMonths0) {
                truncated = true;
                break;
            }

            int totalMonthsForSegment = remainingMonths0 - elapsedTotal;
            BigDecimal newBalance = repaymentCalculator.calculateRemainingBalance(
                    balance, rate, totalMonthsForSegment, segmentMonths, repaymentType
            );

            BigDecimal segmentInterest = previousMonthlyPayment
                    .multiply(BigDecimal.valueOf(segmentMonths))
                    .subtract(balance.subtract(newBalance));
            totalSegmentInterest = totalSegmentInterest.add(segmentInterest);

            elapsedTotal = point.monthOffset();
            rate = point.rate();
            balance = newBalance;

            int newRemainingMonths = remainingMonths0 - elapsedTotal;
            BigDecimal monthlyPayment =
                    repaymentCalculator.calculate(balance, rate, newRemainingMonths, repaymentType).monthlyPayment();

            path.add(new StagedRateStep(elapsedTotal, rate, balance, monthlyPayment, segmentInterest));
            previousMonthlyPayment = monthlyPayment;
        }

        return new CustomPathSimulationResult(initialMonthlyPayment, path, totalSegmentInterest, truncated);
    }

    public record CustomPathSimulationResult(
            BigDecimal initialMonthlyPayment,
            List<StagedRateStep> path,
            BigDecimal totalSegmentInterest,
            boolean truncated
    ) {
    }
}
