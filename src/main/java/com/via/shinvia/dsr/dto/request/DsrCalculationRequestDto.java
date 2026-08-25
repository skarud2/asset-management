package com.via.shinvia.dsr.dto.request;

import com.via.shinvia.dsr.dto.type.*;
import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;

@Getter @Setter
public class DsrCalculationRequestDto {
    @Positive(message = "연소득은 0보다 커야합니다.")
    private BigDecimal annualIncome;    // 간편 계산일 때만 입력

    @NotNull(message = "대출 종류를 선택해주세요.")
    private LoanType loanType;

    @NotNull(message = "희망 대출 금액을 입력하세요.")
    @Positive(message="희망 대출 금액은 0원보다 커야합니다.")
    private BigDecimal requestAmount;

    @NotNull(message="예상 금리를 입력해주세요.")
    @DecimalMin(value="0.0", inclusive=false, message="예상 금리는 0보다 커야합니다.")
    private BigDecimal expectedInterestRate;

    @NotNull(message = "대출 기간을 입력해주세요")
    @Positive(message = "대출 기간은 0보다 커야합니다.")
    private Integer loanTermYears;

    //주담대
    private PropertyRegion propertyRegion;
    private InterestRateType interestRateType;

    //전세자금대출
    private HousingOwnershipType housingOwnershipType;
    private RentalPropertyRegion rentalPropertyRegion;

    //기본값은 원리금균등
    private RepaymentType repaymentType;

}
