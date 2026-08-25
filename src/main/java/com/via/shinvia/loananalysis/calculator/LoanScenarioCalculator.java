package com.via.shinvia.loananalysis.calculator;

import com.via.shinvia.loananalysis.dto.FinancialCapacityDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioRequestDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioResponseDTO;
import com.via.shinvia.loananalysis.type.LoanScenarioType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 대출 대안 계산기
@Component
@RequiredArgsConstructor
public class LoanScenarioCalculator {

    private final LoanPaymentCalculator
            loanPaymentCalculator;


    // 유지 대안 계산
    public LoanScenarioResponseDTO calculateKeep(
            LoanAccountAnalysisDTO loan,
            FinancialCapacityDTO financial
    ) {
        // 잔여기간 계산
        int remainingMonths =
                loanPaymentCalculator
                        .calculateRemainingMonths(
                                loan.getMaturityAt()
                        );

        // 현재 월상환액
        BigDecimal monthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 현재 예상 총이자
        BigDecimal totalInterest =
                loanPaymentCalculator
                        .calculateTotalInterest(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        return LoanScenarioResponseDTO.builder()
                .scenarioType(
                        LoanScenarioType.KEEP
                )
                .scenarioName("현재 유지")
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .beforeBalance(
                        loan.getCurrentBalance()
                )
                .afterBalance(
                        loan.getCurrentBalance()
                )
                .beforeInterestRate(
                        loan.getInterestRate()
                )
                .afterInterestRate(
                        loan.getInterestRate()
                )
                .beforeMonthlyPayment(
                        monthlyPayment
                )
                .afterMonthlyPayment(
                        monthlyPayment
                )
                .repaymentAmount(
                        BigDecimal.ZERO
                )
                .prepaymentFeeAmount(
                        BigDecimal.ZERO
                )
                .refinanceCostAmount(
                        BigDecimal.ZERO
                )
                .remainingCashAmount(
                        defaultZero(
                                financial.getLiquidAssetAmount()
                        )
                )
                .estimatedInterestSaving(
                        BigDecimal.ZERO
                )
                .netBenefitAmount(
                        BigDecimal.ZERO
                )
                .liquidityMonths(
                        calculateLiquidityMonths(
                                financial
                        )
                )
                .recommendationScore(
                        BigDecimal.valueOf(50)
                )
                .recommendationReason(
                        "현재 대출조건과 현금자산을 그대로 유지합니다. "
                                + "예상 잔여이자는 약 "
                                + totalInterest
                                + "원입니다."
                )
                .build();
    }


    // 부분상환 대안 계산
    public LoanScenarioResponseDTO calculatePartialRepayment(
            LoanAccountAnalysisDTO loan,
            FinancialCapacityDTO financial,
            LoanScenarioRequestDTO request
    ) {
        // 현재 현금성 자산
        BigDecimal liquidAsset =
                defaultZero(
                        financial.getLiquidAssetAmount()
                );

        // 남겨둘 비상자금
        BigDecimal emergencyFund =
                defaultZero(
                        request.getEmergencyFundAmount()
                );

        // 실제 사용가능 현금
        BigDecimal availableCash =
                liquidAsset
                        .subtract(emergencyFund)
                        .max(BigDecimal.ZERO);

        // 희망 상환금액
        BigDecimal desiredAmount =
                defaultZero(
                        request.getDesiredRepaymentAmount()
                );

        // 실제 부분상환금액
        BigDecimal repaymentAmount =
                desiredAmount
                        .min(availableCash)
                        .min(
                                defaultZero(
                                        loan.getCurrentBalance()
                                )
                        );

        // 상환 후 잔액
        BigDecimal afterBalance =
                defaultZero(
                        loan.getCurrentBalance()
                )
                        .subtract(repaymentAmount)
                        .max(BigDecimal.ZERO);

        // 잔여기간
        int remainingMonths =
                loanPaymentCalculator
                        .calculateRemainingMonths(
                                loan.getMaturityAt()
                        );

        // 상환 전 월상환액
        BigDecimal beforeMonthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 상환 후 월상환액
        BigDecimal afterMonthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                afterBalance,
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 상환 전 총이자
        BigDecimal beforeInterest =
                loanPaymentCalculator
                        .calculateTotalInterest(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 상환 후 총이자
        BigDecimal afterInterest =
                loanPaymentCalculator
                        .calculateTotalInterest(
                                afterBalance,
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 예상 이자 절감액
        BigDecimal interestSaving =
                beforeInterest
                        .subtract(afterInterest)
                        .max(BigDecimal.ZERO);

        // 중도상환수수료
        BigDecimal prepaymentFee =
                loanPaymentCalculator
                        .calculatePrepaymentFee(
                                repaymentAmount,
                                loan.getPrepaymentFeeRate(),
                                loan.getPrepaymentFeeEndDate()
                        );

        // 순이익
        BigDecimal netBenefit =
                interestSaving
                        .subtract(prepaymentFee);

        // 실행 후 남는 현금
        BigDecimal remainingCash =
                liquidAsset
                        .subtract(repaymentAmount)
                        .max(BigDecimal.ZERO);

        // 추천점수
        BigDecimal recommendationScore =
                calculatePartialScore(
                        netBenefit,
                        remainingCash,
                        emergencyFund
                );

        return LoanScenarioResponseDTO.builder()
                .scenarioType(
                        LoanScenarioType.PARTIAL_REPAYMENT
                )
                .scenarioName("부분상환")
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .beforeBalance(
                        loan.getCurrentBalance()
                )
                .afterBalance(
                        afterBalance
                )
                .beforeInterestRate(
                        loan.getInterestRate()
                )
                .afterInterestRate(
                        loan.getInterestRate()
                )
                .beforeMonthlyPayment(
                        beforeMonthlyPayment
                )
                .afterMonthlyPayment(
                        afterMonthlyPayment
                )
                .repaymentAmount(
                        repaymentAmount
                )
                .prepaymentFeeAmount(
                        prepaymentFee
                )
                .refinanceCostAmount(
                        BigDecimal.ZERO
                )
                .remainingCashAmount(
                        remainingCash
                )
                .estimatedInterestSaving(
                        interestSaving
                )
                .netBenefitAmount(
                        netBenefit
                )
                .liquidityMonths(
                        calculateLiquidityMonths(
                                remainingCash,
                                financial.getTotalExpense()
                        )
                )
                .recommendationScore(
                        recommendationScore
                )
                .recommendationReason(
                        netBenefit.compareTo(
                                BigDecimal.ZERO
                        ) > 0
                                ? "비상자금을 유지하면서 이자비용을 줄일 수 있습니다."
                                : "중도상환수수료로 인해 부분상환 실익이 낮습니다."
                )
                .build();
    }


    // 대환 대안 계산
    public LoanScenarioResponseDTO calculateRefinance(
            LoanAccountAnalysisDTO loan,
            FinancialCapacityDTO financial,
            LoanScenarioRequestDTO request
    ) {
        // 대환금리
        BigDecimal refinanceRate =
                request.getRefinanceInterestRate() == null
                        ? loan.getInterestRate()
                        : request.getRefinanceInterestRate();

        // 현재 잔여기간
        int currentMonths =
                loanPaymentCalculator
                        .calculateRemainingMonths(
                                loan.getMaturityAt()
                        );

        // 대환기간
        int refinanceMonths =
                request.getRefinancePeriodMonths() == null
                        || request.getRefinancePeriodMonths() <= 0
                        ? currentMonths
                        : request.getRefinancePeriodMonths();

        // 현재 월상환액
        BigDecimal beforeMonthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                currentMonths,
                                loan.getRepaymentType()
                        );

        // 대환 후 월상환액
        BigDecimal afterMonthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                loan.getCurrentBalance(),
                                refinanceRate,
                                refinanceMonths,
                                loan.getRepaymentType()
                        );

        // 현재 총이자
        BigDecimal beforeInterest =
                loanPaymentCalculator
                        .calculateTotalInterest(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                currentMonths,
                                loan.getRepaymentType()
                        );

        // 대환 후 총이자
        BigDecimal afterInterest =
                loanPaymentCalculator
                        .calculateTotalInterest(
                                loan.getCurrentBalance(),
                                refinanceRate,
                                refinanceMonths,
                                loan.getRepaymentType()
                        );

        // 이자 절감액
        BigDecimal interestSaving =
                beforeInterest.subtract(
                        afterInterest
                );

        // 기존 대출 상환수수료
        BigDecimal prepaymentFee =
                loanPaymentCalculator
                        .calculatePrepaymentFee(
                                loan.getCurrentBalance(),
                                loan.getPrepaymentFeeRate(),
                                loan.getPrepaymentFeeEndDate()
                        );

        // 대환 부대비용
        BigDecimal refinanceCost =
                defaultZero(
                        request.getRefinanceCostAmount()
                );

        // 대환 순이익
        BigDecimal netBenefit =
                interestSaving
                        .subtract(prepaymentFee)
                        .subtract(refinanceCost);

        // 남는 현금
        BigDecimal remainingCash =
                defaultZero(
                        financial.getLiquidAssetAmount()
                )
                        .subtract(refinanceCost)
                        .subtract(prepaymentFee)
                        .max(BigDecimal.ZERO);

        // 추천점수
        BigDecimal recommendationScore =
                calculateRefinanceScore(
                        loan.getInterestRate(),
                        refinanceRate,
                        netBenefit
                );

        return LoanScenarioResponseDTO.builder()
                .scenarioType(
                        LoanScenarioType.REFINANCE
                )
                .scenarioName("대환")
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .beforeBalance(
                        loan.getCurrentBalance()
                )
                .afterBalance(
                        loan.getCurrentBalance()
                )
                .beforeInterestRate(
                        loan.getInterestRate()
                )
                .afterInterestRate(
                        refinanceRate
                )
                .beforeMonthlyPayment(
                        beforeMonthlyPayment
                )
                .afterMonthlyPayment(
                        afterMonthlyPayment
                )
                .repaymentAmount(
                        BigDecimal.ZERO
                )
                .prepaymentFeeAmount(
                        prepaymentFee
                )
                .refinanceCostAmount(
                        refinanceCost
                )
                .remainingCashAmount(
                        remainingCash
                )
                .estimatedInterestSaving(
                        interestSaving
                )
                .netBenefitAmount(
                        netBenefit
                )
                .liquidityMonths(
                        calculateLiquidityMonths(
                                remainingCash,
                                financial.getTotalExpense()
                        )
                )
                .recommendationScore(
                        recommendationScore
                )
                .recommendationReason(
                        netBenefit.compareTo(
                                BigDecimal.ZERO
                        ) > 0
                                ? "대환비용을 제외해도 이자 절감효과가 있습니다."
                                : "수수료와 부대비용을 고려하면 대환 실익이 낮습니다."
                )
                .build();
    }


    // 현금보유 대안 계산
    public LoanScenarioResponseDTO calculateCashHolding(
            LoanAccountAnalysisDTO loan,
            FinancialCapacityDTO financial
    ) {
        // 현금성 자산
        BigDecimal liquidAsset =
                defaultZero(
                        financial.getLiquidAssetAmount()
                );

        // 현금 유지 가능개월
        BigDecimal liquidityMonths =
                calculateLiquidityMonths(
                        financial
                );

        // 잔여기간
        int remainingMonths =
                loanPaymentCalculator
                        .calculateRemainingMonths(
                                loan.getMaturityAt()
                        );

        // 현재 월상환액
        BigDecimal monthlyPayment =
                loanPaymentCalculator
                        .calculateMonthlyPayment(
                                loan.getCurrentBalance(),
                                loan.getInterestRate(),
                                remainingMonths,
                                loan.getRepaymentType()
                        );

        // 현금보유 추천점수
        BigDecimal recommendationScore =
                liquidityMonths.compareTo(
                        BigDecimal.valueOf(3)
                ) < 0
                        ? BigDecimal.valueOf(90)
                        : BigDecimal.valueOf(45);

        return LoanScenarioResponseDTO.builder()
                .scenarioType(
                        LoanScenarioType.CASH_HOLDING
                )
                .scenarioName("현금보유")
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .beforeBalance(
                        loan.getCurrentBalance()
                )
                .afterBalance(
                        loan.getCurrentBalance()
                )
                .beforeInterestRate(
                        loan.getInterestRate()
                )
                .afterInterestRate(
                        loan.getInterestRate()
                )
                .beforeMonthlyPayment(
                        monthlyPayment
                )
                .afterMonthlyPayment(
                        monthlyPayment
                )
                .repaymentAmount(
                        BigDecimal.ZERO
                )
                .prepaymentFeeAmount(
                        BigDecimal.ZERO
                )
                .refinanceCostAmount(
                        BigDecimal.ZERO
                )
                .remainingCashAmount(
                        liquidAsset
                )
                .estimatedInterestSaving(
                        BigDecimal.ZERO
                )
                .netBenefitAmount(
                        BigDecimal.ZERO
                )
                .liquidityMonths(
                        liquidityMonths
                )
                .recommendationScore(
                        recommendationScore
                )
                .recommendationReason(
                        liquidityMonths.compareTo(
                                BigDecimal.valueOf(3)
                        ) < 0
                                ? "생활비 3개월분이 부족하여 현금보유가 우선입니다."
                                : "비상자금은 확보되어 있어 상환 대안도 검토할 수 있습니다."
                )
                .build();
    }


    // 부분상환 추천점수
    private BigDecimal calculatePartialScore(
            BigDecimal netBenefit,
            BigDecimal remainingCash,
            BigDecimal emergencyFund
    ) {
        // 손해 발생
        if (netBenefit.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.valueOf(30);
        }

        // 비상자금 부족
        if (remainingCash.compareTo(
                emergencyFund
        ) < 0) {
            return BigDecimal.valueOf(40);
        }

        return BigDecimal.valueOf(80);
    }


    // 대환 추천점수
    private BigDecimal calculateRefinanceScore(
            BigDecimal currentRate,
            BigDecimal refinanceRate,
            BigDecimal netBenefit
    ) {
        // 금리인하 없음
        if (defaultZero(refinanceRate)
                .compareTo(
                        defaultZero(currentRate)
                ) >= 0) {

            return BigDecimal.valueOf(20);
        }

        // 순이익 없음
        if (netBenefit.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return BigDecimal.valueOf(30);
        }

        return BigDecimal.valueOf(85);
    }


    // 현금 유지 가능개월 계산
    private BigDecimal calculateLiquidityMonths(
            FinancialCapacityDTO financial
    ) {
        return calculateLiquidityMonths(
                financial.getLiquidAssetAmount(),
                financial.getTotalExpense()
        );
    }


    // 현금 유지 가능개월 계산
    private BigDecimal calculateLiquidityMonths(
            BigDecimal cash,
            BigDecimal monthlyExpense
    ) {
        BigDecimal safeCash =
                defaultZero(cash);

        BigDecimal safeExpense =
                defaultZero(monthlyExpense);

        // 지출정보 없음
        if (safeExpense.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.ZERO;
        }

        return safeCash.divide(
                safeExpense,
                2,
                RoundingMode.HALF_UP
        );
    }


    // null 금액 처리
    private BigDecimal defaultZero(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}
