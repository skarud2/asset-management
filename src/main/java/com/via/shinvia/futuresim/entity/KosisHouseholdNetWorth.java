package com.via.shinvia.futuresim.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// kosis_household_net_worth 1건 (KOSIS 가계금융복지조사, 가구원수별 소득/자산/부채/순자산 벤치마크)
@Getter
@Setter
@NoArgsConstructor
public class KosisHouseholdNetWorth {

    private Long id;

    private String householdSizeCode;

    private String householdSizeLabel;

    private String surveyYear;

    private BigDecimal avgIncome;

    private BigDecimal medianIncome;

    private BigDecimal avgAsset;

    private BigDecimal medianAsset;

    private BigDecimal avgDebt;

    private BigDecimal medianDebt;

    private BigDecimal avgNetWorth;

    private BigDecimal medianNetWorth;

    private BigDecimal avgDebtRepayment;

    private BigDecimal medianDebtRepayment;

    private BigDecimal avgHouseholdHeadAge;

    private BigDecimal householdDistributionPct;
}
