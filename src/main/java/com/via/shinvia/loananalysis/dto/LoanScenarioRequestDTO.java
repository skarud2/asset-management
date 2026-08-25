package com.via.shinvia.loananalysis.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 대출 대안 비교 요청값
@Getter
@Setter
public class LoanScenarioRequestDTO {

    // 분석할 대출 식별자
    @NotNull(message = "대상 대출을 선택해주세요.")
    @Positive(message = "대출 식별자는 양수여야 합니다.")
    private Long targetLoanAccountId;

    // 부분상환 희망금액
    @PositiveOrZero(message = "부분상환금액은 0원 이상이어야 합니다.")
    private BigDecimal desiredRepaymentAmount;

    // 반드시 남길 비상자금
    @PositiveOrZero(message = "비상자금은 0원 이상이어야 합니다.")
    private BigDecimal emergencyFundAmount;

    // 대환 예상금리
    @PositiveOrZero(message = "대환금리는 0% 이상이어야 합니다.")
    @DecimalMax(value = "100.0", message = "대환금리는 100% 이하여야 합니다.")
    private BigDecimal refinanceInterestRate;

    // 대환 부대비용
    @PositiveOrZero(message = "대환 부대비용은 0원 이상이어야 합니다.")
    private BigDecimal refinanceCostAmount;

    // 대환 후 상환기간
    @Positive(message = "대환 기간은 1개월 이상이어야 합니다.")
    @Max(value = 600, message = "대환 기간은 600개월 이하여야 합니다.")
    private Integer refinancePeriodMonths;
}
