package com.via.shinvia.loan.ratesimulation.breakeven.service;

import com.via.shinvia.loan.ratesimulation.breakeven.dto.request.BreakevenRateRequest;
import com.via.shinvia.loan.ratesimulation.breakeven.dto.response.BreakevenRateResponse;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.breakeven.exception.UnsupportedThresholdTypeException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.breakeven.type.ThresholdType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

// loan_account 조회 -> 상태 검증 -> 계산 엔진 호출 -> 응답 조립을 담당하는 오케스트레이션 서비스
@Service
public class LoanBreakevenRateService {

    private static final String NORMAL_LOAN_STATUS = "정상";

    private final LoanAccountSummaryMapper loanAccountSummaryMapper;
    private final LoanRepaymentCalculator repaymentCalculator;
    private final BreakevenRateCalculator breakevenRateCalculator;

    public LoanBreakevenRateService(
            LoanAccountSummaryMapper loanAccountSummaryMapper,
            LoanRepaymentCalculator repaymentCalculator,
            BreakevenRateCalculator breakevenRateCalculator
    ) {
        this.loanAccountSummaryMapper = loanAccountSummaryMapper;
        this.repaymentCalculator = repaymentCalculator;
        this.breakevenRateCalculator = breakevenRateCalculator;
    }

    public BreakevenRateResponse calculate(BreakevenRateRequest request) {
        if (request.thresholdType() != ThresholdType.MONTHLY_PAYMENT_AMOUNT) {
            throw new UnsupportedThresholdTypeException(request.thresholdType().name());
        }

        LoanAccountSummary loan = loanAccountSummaryMapper.findById(request.loanId());
        if (loan == null) {
            throw new LoanNotFoundException(request.loanId());
        }
        if (!NORMAL_LOAN_STATUS.equals(loan.getLoanStatus())) {
            throw new InvalidLoanStatusException();
        }

        int remainingMonths = repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt());

        BreakevenRateCalculator.Result result = breakevenRateCalculator.search(
                loan.getCurrentBalance(),
                loan.getInterestRate(),
                remainingMonths,
                loan.getRepaymentType(),
                request.thresholdValue()
        );

        BigDecimal marginPercent = result.reached()
                ? result.breakevenRate().subtract(loan.getInterestRate()).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new BreakevenRateResponse(
                loan.getLoanAccountId(),
                loan.getLoanType(),
                loan.getRateType(),
                loan.getInterestRate(),
                result.currentMonthlyPayment(),
                result.breakevenRate(),
                marginPercent,
                request.thresholdType(),
                request.thresholdValue(),
                result.alreadyExceeded(),
                result.excessAmount(),
                buildMessage(result, marginPercent, request.thresholdValue())
        );
    }

    private String buildMessage(BreakevenRateCalculator.Result result, BigDecimal marginPercent, BigDecimal thresholdValue) {
        if (result.alreadyExceeded()) {
            return "이미 현재 금리에서 월상환액이 기준(" + formatMoney(thresholdValue) + "원)을 초과했어요 (초과액: "
                    + formatMoney(result.excessAmount()) + "원)";
        }
        if (!result.reached()) {
            return "금리가 10%p 올라도 월상환액이 기준(" + formatMoney(thresholdValue) + "원)에 도달하지 않아요";
        }
        return "기준금리가 지금보다 " + marginPercent + "%p 더 오르면 월상환액이 " + formatMoney(thresholdValue) + "원을 넘어요";
    }

    private String formatMoney(BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(value);
    }
}
