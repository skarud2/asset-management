package com.via.shinvia.stresstest.service;

import com.via.shinvia.loan.ratesimulation.common.dto.response.RepaymentCalculationResult;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 보유 대출(loan_status='정상') 전체에 금리 상승 스트레스를 적용했을 때의 월상환액 합산.
// 변동금리만 rateDeltaPercent를 가산하고, 고정금리는 현재 금리 그대로 계산한다(스트레스 영향 없음).
@Service
public class LoanBurdenAggregator {

    private static final String VARIABLE_RATE_TYPE = "변동";
    private static final int MONEY_SCALE = 2;

    private final StressTestLoanMapper loanMapper;
    private final LoanRepaymentCalculator repaymentCalculator;

    public LoanBurdenAggregator(
            StressTestLoanMapper loanMapper,
            LoanRepaymentCalculator repaymentCalculator
    ) {
        this.loanMapper = loanMapper;
        this.repaymentCalculator = repaymentCalculator;
    }

    public Result aggregate(Long userId, BigDecimal rateDeltaPercent) {
        List<StressTestLoanRow> loans = loanMapper.findNormalLoansByUserId(userId);

        BigDecimal totalStressed = zero();
        BigDecimal totalCurrent = zero();

        for (StressTestLoanRow loan : loans) {
            int remainingMonths = repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt());

            BigDecimal stressedRate = VARIABLE_RATE_TYPE.equals(loan.getRateType())
                    ? loan.getInterestRate().add(rateDeltaPercent)
                    : loan.getInterestRate();

            RepaymentCalculationResult stressed = repaymentCalculator.calculate(
                    loan.getCurrentBalance(), stressedRate, remainingMonths, loan.getRepaymentType()
            );
            RepaymentCalculationResult current = repaymentCalculator.calculate(
                    loan.getCurrentBalance(), loan.getInterestRate(), remainingMonths, loan.getRepaymentType()
            );

            totalStressed = totalStressed.add(stressed.monthlyPayment());
            totalCurrent = totalCurrent.add(current.monthlyPayment());
        }

        return new Result(totalStressed, totalCurrent);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record Result(
            BigDecimal totalStressedLoanPayment,
            BigDecimal totalCurrentLoanPayment
    ) {
    }
}
