package com.via.shinvia.lifecycle.scenario.dto;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleEventSnapshotDto {

    private Integer eventOrder;
    private Long lifecycleEventId;
    private LifecycleEventType eventType;
    private LocalDate eventDate;

    private BigDecimal eventCost;
    private BigDecimal supportBenefit;
    private BigDecimal fundingShortage;

    private BigDecimal beforeCashAsset;
    private BigDecimal afterCashAsset;
    private BigDecimal cashAssetChange;

    private BigDecimal beforeHousingAsset;
    private BigDecimal afterHousingAsset;

    private BigDecimal beforeRealEstateAsset;
    private BigDecimal afterRealEstateAsset;

    private BigDecimal beforeDepositAsset;
    private BigDecimal afterDepositAsset;

    private String beforeCurrentHousingType;
    private String afterCurrentHousingType;

    private BigDecimal beforeTotalDebt;
    private BigDecimal afterTotalDebt;
    private BigDecimal totalDebtChange;

    private java.util.List<com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto> loans;

    private BigDecimal beforeNetAsset;
    private BigDecimal afterNetAsset;
    private BigDecimal netAssetChange;

    private BigDecimal beforeMonthlySavingCapacity;
    private BigDecimal afterMonthlySavingCapacity;
    private BigDecimal monthlySavingCapacityChange;

    private BigDecimal beforeDsr;
    private BigDecimal afterDsr;
    private BigDecimal dsrChange;

    private LifestyleLevel lifestyleLevel;

    private Integer childOrder;
    private Boolean repurchaseCarSeat;
    private Boolean repurchaseStroller;
    private Boolean repurchaseCrib;
    private Boolean repurchaseOtherSetup;
    private Boolean postpartumCare;
    private String childbirthRegionSido;
    private String childbirthRegionSigungu;

    private BigDecimal estimatedCost;
    private BigDecimal userRequiredAmount;
    private BigDecimal userContributionAmount;
    private BigDecimal additionalMonthlyExpense;
    private BigDecimal cashInflowAmount;
    private BigDecimal familySupportAmount;
    private BigDecimal marriageHallCost;
    private BigDecimal marriageMealCost;
    private BigDecimal marriageFurnitureCost;
    private BigDecimal marriageHoneymoonCost;
    private BigDecimal postpartumCareCost;
    private BigDecimal infantCarSeatCost;
    private BigDecimal infantStrollerCost;
    private BigDecimal infantCribCost;
    private BigDecimal infantOtherSetupCost;
    private BigDecimal newLoanAmount;
    private BigDecimal newLoanMonthlyPayment;
    private BigDecimal monthlyLoanPrincipal;
    private BigDecimal monthlyLoanInterest;
    private BigDecimal loanInterestRate;
    private Integer loanPeriodMonths;
    private String loanRepaymentType;
    private BigDecimal acquiredAssetAmount;
    private BigDecimal taxAmount;
    private BigDecimal brokerageFeeAmount;
    private BigDecimal registrationFeeAmount;

    private List<LifecycleSupportDto> supports;
    private List<LifecycleProductDto> recommendedProducts;

    private LifecycleFeasibilityDto feasibility;

    private String summary;


}
