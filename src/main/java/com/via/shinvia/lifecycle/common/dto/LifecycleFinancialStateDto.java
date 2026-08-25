package com.via.shinvia.lifecycle.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleFinancialStateDto {

    // 기준 시점 (현재 or 미래 특정 이벤트 시점)
    private LocalDate stateDate;

    // [자산 및 부채]
    @Builder.Default
    private BigDecimal cashAsset = BigDecimal.ZERO;          // 통장 현금/유동자산 (지출/저축의 기준)

    @Builder.Default
    private BigDecimal realEstateAsset = BigDecimal.ZERO;    // 소유 주택(부동산) 자산 가치

    @Builder.Default
    private BigDecimal depositAsset = BigDecimal.ZERO;       // 임차 보증금(전세/월세 보증금) 자산 가치

    @Builder.Default
    private BigDecimal housingAsset = BigDecimal.ZERO;       // 하위 호환용 총 주거자산 (realEstate + deposit)

    private String currentHousingType;                       // 현재 거주 형태 (FAMILY, MONTHLY_RENT, JEONSE, OWN 등)

    @Builder.Default
    private BigDecimal totalDebt = BigDecimal.ZERO;          // 총 부채 잔액

    @Builder.Default
    private java.util.List<LifecycleLoanDto> loans = new java.util.ArrayList<>(); // 보유 개별 대출 목록

    // [소득 및 지원금]
    @Builder.Default
    private BigDecimal annualIncome = BigDecimal.ZERO;       // 예상 연소득

    @Builder.Default
    private BigDecimal monthlySupportIncome = BigDecimal.ZERO; // 월 정부/복지지원금

    // [월 지출 영역]
    @Builder.Default
    private BigDecimal monthlyLivingExpense = BigDecimal.ZERO; // 월 생활비

    @Builder.Default
    private BigDecimal monthlyHousingExpense = BigDecimal.ZERO; // 월 주거비 (월세, 관리비)

    @Builder.Default
    private BigDecimal monthlyDebtPayment = BigDecimal.ZERO;   // 계산기(Calculator)가 산출한 월 대출상환액

    // [최종 산출 지표]
    @Builder.Default
    private BigDecimal monthlySavingCapacity = BigDecimal.ZERO; // 예상 월 저축 가능금액

    @Builder.Default
    private BigDecimal dsr = BigDecimal.ZERO;                   // DSR 계산 모듈에서 산출된 값

    /**
     * 총 주거자산 (부동산 + 보증금)
     */
    public BigDecimal getHousingAsset() {
        BigDecimal sum = nvl(realEstateAsset).add(nvl(depositAsset));
        if (sum.compareTo(BigDecimal.ZERO) > 0) {
            return sum;
        }
        return nvl(housingAsset);
    }

    /**
     * 순자산 = (현금 + 부동산 + 임차보증금) - 총부채
     */
    public BigDecimal getNetAsset() {
        return nvl(cashAsset)
                .add(getHousingAsset())
                .subtract(nvl(totalDebt));
    }

    /**
     * 월 저축여력 재계산
     * 저축여력 = (연소득 / 12) + 월지원금 - 월생활비 - 월주거비 - 월대출상환액
     */
    public void recalculateMonthlySavingCapacity() {
        BigDecimal monthlyIncome = nvl(annualIncome).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        this.monthlySavingCapacity = monthlyIncome
                .add(nvl(monthlySupportIncome))
                .subtract(nvl(monthlyLivingExpense))
                .subtract(nvl(monthlyHousingExpense))
                .subtract(nvl(monthlyDebtPayment));
    }

    /**
     * 새 시점으로 복제 (대출 목록 포함)
     */
    public LifecycleFinancialStateDto copy(LocalDate newDate) {
        java.util.List<LifecycleLoanDto> copiedLoans = new java.util.ArrayList<>();
        if (this.loans != null) {
            for (LifecycleLoanDto loan : this.loans) {
                if (loan != null) {
                    copiedLoans.add(LifecycleLoanDto.builder()
                            .loanAccountId(loan.getLoanAccountId())
                            .loanType(loan.getLoanType())
                            .currentBalance(loan.getCurrentBalance())
                            .interestRate(loan.getInterestRate())
                            .rateType(loan.getRateType())
                            .repaymentType(loan.getRepaymentType())
                            .maturityAt(loan.getMaturityAt())
                            .prepaymentFeeRate(loan.getPrepaymentFeeRate())
                            .prepaymentFeeEndDate(loan.getPrepaymentFeeEndDate())
                            .build());
                }
            }
        }
        return this.toBuilder()
                .stateDate(newDate)
                .loans(copiedLoans)
                .build();
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}