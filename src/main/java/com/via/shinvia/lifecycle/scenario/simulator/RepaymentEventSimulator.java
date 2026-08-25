package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [7. 대출 조기상환 생애주기 시뮬레이터]
 * - 고금리 대출 우선 상환, 중도상환수수료(0.65%), DSR 대폭 개선 매핑
 * - 값 부재 시 0 또는 빈 문자열을 기본으로 안전하게 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentEventSimulator implements LifecycleEventSimulator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LifecycleReferenceService referenceService;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.REPAYMENT;
    }

    @Override
    public LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal repayAmount = input.getEstimatedCost() != null ? input.getEstimatedCost() : BigDecimal.ZERO;

        // 중도상환수수료율 매핑 (기본 0.65%)
        BigDecimal feeRate = resolvePrepaymentFeeRate();
        BigDecimal prepaymentFee = repayAmount.multiply(feeRate);
        BigDecimal totalRequired = repayAmount.add(prepaymentFee);

        BigDecimal actualRepayPrincipal = repayAmount;
        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 보유 현금 내에서 상환액 차감
        if (beforeCash.compareTo(totalRequired) >= 0) {
            afterCash = beforeCash.subtract(totalRequired);
            summary = String.format("대출 조기상환으로 원금 %s원(수수료 %s원)이 상환되었습니다.",
                    formatMoney(repayAmount), formatMoney(prepaymentFee));
        } else {
            fundingShortage = totalRequired.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            // 보유 현금만큼만 최대한 부분 상환
            BigDecimal maxAffordable = beforeCash.divide(BigDecimal.ONE.add(feeRate), 0, RoundingMode.DOWN);
            actualRepayPrincipal = maxAffordable;
            prepaymentFee = maxAffordable.multiply(feeRate);
            summary = String.format("보유 현금 부족으로 %s원만 부분 상환되었습니다. (부족액: %s원)",
                    formatMoney(actualRepayPrincipal), formatMoney(fundingShortage));
        }

        // 2. 부채 삭감 및 월 상환액 갱신
        List<LifecycleLoanDto> updatedLoans = new ArrayList<>();
        if (beforeState.getLoans() != null) {
            for (var l : beforeState.getLoans()) {
                if (l != null) updatedLoans.add(l.toBuilder().build());
            }
        }

        BigDecimal remainingRepay = actualRepayPrincipal;

        // 특정 대출 지정 상환 또는 고금리 순 상환
        if (input.getTargetLoanAccountId() != null) {
            for (LifecycleLoanDto loan : updatedLoans) {
                if (input.getTargetLoanAccountId().equals(loan.getLoanAccountId())) {
                    BigDecimal balance = nvl(loan.getCurrentBalance());
                    BigDecimal deduct = balance.min(remainingRepay);
                    loan.setCurrentBalance(balance.subtract(deduct));
                    remainingRepay = remainingRepay.subtract(deduct);
                    break;
                }
            }
        }

        // 잔여 상환금이 있으면 고금리 대출부터 우선 차감
        if (remainingRepay.compareTo(BigDecimal.ZERO) > 0) {
            updatedLoans.sort(Comparator.comparing(LifecycleLoanDto::getInterestRate, Comparator.nullsLast(Comparator.reverseOrder())));
            for (LifecycleLoanDto loan : updatedLoans) {
                if (remainingRepay.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal balance = nvl(loan.getCurrentBalance());
                if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal deduct = balance.min(remainingRepay);
                loan.setCurrentBalance(balance.subtract(deduct));
                remainingRepay = remainingRepay.subtract(deduct);
            }
        }

        // 잔액이 0 초과인 대출만 유지 및 총부채/월상환액 재계산
        List<LifecycleLoanDto> activeLoans = new ArrayList<>();
        BigDecimal newTotalDebt = BigDecimal.ZERO;
        BigDecimal newMonthlyDebtPayment = BigDecimal.ZERO;

        for (LifecycleLoanDto loan : updatedLoans) {
            if (loan.getCurrentBalance() != null && loan.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
                activeLoans.add(loan);
                newTotalDebt = newTotalDebt.add(loan.getCurrentBalance());
                BigDecimal rate = loan.getInterestRate() != null ? loan.getInterestRate() : new BigDecimal("0.05");
                newMonthlyDebtPayment = newMonthlyDebtPayment.add(loan.getCurrentBalance().multiply(rate).divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP));
            }
        }

        // 3. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .totalDebt(newTotalDebt)
                .monthlyDebtPayment(newMonthlyDebtPayment)
                .loans(activeLoans)
                .build();

        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newMonthlyDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[RepaymentEventSimulator] Simulated. Repaid: {}, RemainingDebt: {}, NewDSR: {}%",
                actualRepayPrincipal, newTotalDebt, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.REPAYMENT)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalRequired)
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary != null ? summary : "")
                .build();
    }

    private BigDecimal resolvePrepaymentFeeRate() {
        try {
            return referenceService.getNationalRate(
                    LifecycleEventType.REPAYMENT,
                    "PREPAYMENT_FEE_RATE",
                    null
            );
        } catch (Exception e) {
            return new BigDecimal("0.006500");
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}