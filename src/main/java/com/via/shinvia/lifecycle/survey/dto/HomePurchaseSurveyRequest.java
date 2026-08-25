package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.HousingType;
import com.via.shinvia.lifecycle.common.model.LifecycleRepaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePurchaseSurveyRequest {

    // 주택 구매 희망일
    private LocalDate targetDate;

    // 희망 지역
    private String regionSido;

    private String regionSigungu;

    // 아파트, 빌라 등
    private HousingType housingType;

    // 희망 전용면적
    private BigDecimal desiredArea;

    // 주택 가격 수준
    private LifestyleLevel lifestyleLevel;

    // 사용자가 직접 생각하고 있는 주택가격
    // 기준자료 사용 시 null 가능
    private BigDecimal desiredPurchasePrice;

    // 주택구매에 사용할 자기자금
    private BigDecimal ownFundAmount;

    // 희망 주택담보대출 기간
    private Integer loanPeriodMonths;

    // 희망 상환방식
    // EQUAL_PAYMENT, EQUAL_PRINCIPAL 등
    private LifecycleRepaymentType repaymentType;
}
