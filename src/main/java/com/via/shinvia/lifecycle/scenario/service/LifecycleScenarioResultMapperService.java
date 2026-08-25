package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleEventSnapshotDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LifecycleScenarioResultMapperService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final LifecycleFeasibilityService lifecycleFeasibilityService;

    public LifecycleScenarioResultMapperService(
            LifecycleFeasibilityService lifecycleFeasibilityService
    ) {
        this.lifecycleFeasibilityService = lifecycleFeasibilityService;
    }

    public LifecycleScenarioResultDto toScenarioResult(
            Long scenarioId,
            Long userId,
            LifecycleFinancialStateDto initialState,
            List<LifecycleEventInput> eventInputs,
            List<LifecycleEventResult> eventResults
    ) {
        List<LifecycleEventResult> safeResults = eventResults != null
                ? eventResults
                : List.of();

        List<LifecycleEventInput> safeInputs = eventInputs != null
                ? eventInputs
                : List.of();

        Map<Long, LifecycleEventInput> inputMap = safeInputs.stream()
                .filter(input -> input != null && input.getLifecycleEventId() != null)
                .collect(Collectors.toMap(
                        LifecycleEventInput::getLifecycleEventId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<LifecycleEventSnapshotDto> snapshots = new ArrayList<>();

        for (int i = 0; i < safeResults.size(); i++) {
            LifecycleEventResult result = safeResults.get(i);

            if (result == null) {
                continue;
            }

            LifecycleEventInput input = inputMap.get(result.getLifecycleEventId());
            snapshots.add(toSnapshot(i + 1, result, input));
        }

        LifecycleFinancialStateDto finalState = findFinalState(initialState, safeResults);

        return LifecycleScenarioResultDto.builder()
                .scenarioId(scenarioId)
                .userId(userId)
                .simulatedAt(LocalDateTime.now())
                .initialState(initialState)
                .finalState(finalState)
                .eventCount(snapshots.size())
                .totalEventCost(sumEventCost(safeResults))
                .totalSupportBenefit(sumSupportBenefit(safeResults))
                .totalFundingShortage(sumFundingShortage(safeResults))
                .initialNetAsset(netAsset(initialState))
                .finalNetAsset(netAsset(finalState))
                .netAssetChange(change(netAsset(initialState), netAsset(finalState)))
                .initialCashAsset(cashAsset(initialState))
                .finalCashAsset(cashAsset(finalState))
                .initialTotalDebt(totalDebt(initialState))
                .finalTotalDebt(totalDebt(finalState))
                .finalMonthlySavingCapacity(monthlySavingCapacity(finalState))
                .finalDsr(dsr(finalState))
                .eventSnapshots(snapshots)
                .build();
    }

    private LifecycleEventSnapshotDto toSnapshot(
            Integer eventOrder,
            LifecycleEventResult result,
            LifecycleEventInput input
    ) {
        LifecycleFinancialStateDto beforeState = result.getBeforeState();
        LifecycleFinancialStateDto afterState = result.getAfterState();
        BigDecimal monthlyLoanPayment = input != null && input.getNewLoanAmount() != null
                ? change(afterState != null ? afterState.getMonthlyDebtPayment() : ZERO,
                beforeState != null ? beforeState.getMonthlyDebtPayment() : ZERO)
                : ZERO;
        if (monthlyLoanPayment.signum() <= 0) {
            monthlyLoanPayment = calculateExpectedMonthlyLoanPayment(input);
        }
        BigDecimal monthlyLoanInterest = calculateMonthlyLoanInterest(input, monthlyLoanPayment);

        return LifecycleEventSnapshotDto.builder()
                .eventOrder(eventOrder)
                .lifecycleEventId(result.getLifecycleEventId())
                .eventType(result.getEventType())
                .eventDate(result.getEventDate())
                .eventCost(nvl(result.getEventCost()))
                .supportBenefit(nvl(result.getSupportBenefit()))
                .fundingShortage(nvl(result.getFundingShortage()))
                .beforeCashAsset(cashAsset(beforeState))
                .afterCashAsset(cashAsset(afterState))
                .cashAssetChange(change(cashAsset(beforeState), cashAsset(afterState)))
                .beforeHousingAsset(housingAsset(beforeState))
                .afterHousingAsset(housingAsset(afterState))
                .beforeRealEstateAsset(beforeState != null ? nvl(beforeState.getRealEstateAsset()) : ZERO)
                .afterRealEstateAsset(afterState != null ? nvl(afterState.getRealEstateAsset()) : ZERO)
                .beforeDepositAsset(beforeState != null ? nvl(beforeState.getDepositAsset()) : ZERO)
                .afterDepositAsset(afterState != null ? nvl(afterState.getDepositAsset()) : ZERO)
                .beforeCurrentHousingType(beforeState != null ? beforeState.getCurrentHousingType() : null)
                .afterCurrentHousingType(afterState != null ? afterState.getCurrentHousingType() : null)
                .beforeTotalDebt(totalDebt(beforeState))
                .afterTotalDebt(totalDebt(afterState))
                .totalDebtChange(change(totalDebt(beforeState), totalDebt(afterState)))
                .loans(afterState != null && afterState.getLoans() != null ? afterState.getLoans() : List.of())
                .beforeNetAsset(netAsset(beforeState))
                .afterNetAsset(netAsset(afterState))
                .netAssetChange(change(netAsset(beforeState), netAsset(afterState)))
                .beforeMonthlySavingCapacity(monthlySavingCapacity(beforeState))
                .afterMonthlySavingCapacity(monthlySavingCapacity(afterState))
                .monthlySavingCapacityChange(change(
                        monthlySavingCapacity(beforeState),
                        monthlySavingCapacity(afterState)
                ))
                .beforeDsr(dsr(beforeState))
                .afterDsr(dsr(afterState))
                .dsrChange(change(dsr(beforeState), dsr(afterState)))
                .lifestyleLevel(input != null ? input.getLifestyleLevel() : null)
                .childOrder(input != null ? input.getChildOrder() : null)
                .repurchaseCarSeat(input != null ? input.getRepurchaseCarSeat() : null)
                .repurchaseStroller(input != null ? input.getRepurchaseStroller() : null)
                .repurchaseCrib(input != null ? input.getRepurchaseCrib() : null)
                .repurchaseOtherSetup(input != null ? input.getRepurchaseOtherSetup() : null)
                .postpartumCare(input != null ? input.getPostpartumCare() : null)
                .childbirthRegionSido(input != null ? input.getChildbirthRegionSido() : null)
                .childbirthRegionSigungu(input != null ? input.getChildbirthRegionSigungu() : null)
                .estimatedCost(input != null ? nvl(input.getEstimatedCost()) : ZERO)
                .userRequiredAmount(input != null ? nvl(input.getUserRequiredAmount()) : ZERO)
                .userContributionAmount(input != null
                        ? nvl(input.getUserContributionAmount())
                        : ZERO)
                .additionalMonthlyExpense(input != null ? nvl(input.getAdditionalMonthlyExpense()) : ZERO)
                .cashInflowAmount(input != null ? nvl(input.getCashInflowAmount()) : ZERO)
                .familySupportAmount(input != null ? nvl(input.getFamilySupportAmount()) : ZERO)
                .marriageHallCost(input != null ? nvl(input.getMarriageHallCost()) : ZERO)
                .marriageMealCost(input != null ? nvl(input.getMarriageMealCost()) : ZERO)
                .marriageFurnitureCost(input != null ? nvl(input.getMarriageFurnitureCost()) : ZERO)
                .marriageHoneymoonCost(input != null ? nvl(input.getMarriageHoneymoonCost()) : ZERO)
                .postpartumCareCost(input != null ? nvl(input.getPostpartumCareCost()) : ZERO)
                .infantCarSeatCost(input != null ? nvl(input.getInfantCarSeatCost()) : ZERO)
                .infantStrollerCost(input != null ? nvl(input.getInfantStrollerCost()) : ZERO)
                .infantCribCost(input != null ? nvl(input.getInfantCribCost()) : ZERO)
                .infantOtherSetupCost(input != null ? nvl(input.getInfantOtherSetupCost()) : ZERO)
                .newLoanAmount(input != null ? nvl(input.getNewLoanAmount()) : ZERO)
                .newLoanMonthlyPayment(monthlyLoanPayment)
                .monthlyLoanInterest(monthlyLoanInterest)
                .monthlyLoanPrincipal(monthlyLoanPayment.subtract(monthlyLoanInterest).max(ZERO))
                .loanInterestRate(input != null ? input.getLoanInterestRate() : null)
                .loanPeriodMonths(input != null ? input.getLoanPeriodMonths() : null)
                .loanRepaymentType(input != null && input.getNewLoanAmount() != null
                        && input.getNewLoanAmount().compareTo(ZERO) > 0
                        ? repaymentTypeLabel(input.getLoanRepaymentType())
                        : null)
                .acquiredAssetAmount(input != null ? nvl(input.getAcquiredAssetAmount()) : ZERO)
                .taxAmount(input != null ? nvl(input.getTaxAmount()) : ZERO)
                .brokerageFeeAmount(input != null ? nvl(input.getBrokerageFeeAmount()) : ZERO)
                .registrationFeeAmount(input != null ? nvl(input.getRegistrationFeeAmount()) : ZERO)
                .supports(input != null && input.getSupports() != null
                        ? input.getSupports()
                        : List.of())
                .recommendedProducts(input != null && input.getRecommendedProducts() != null
                        ? input.getRecommendedProducts()
                        : List.of())
                .feasibility(lifecycleFeasibilityService.assess(result))
                .summary(result.getSummary())
                .build();
    }

    private BigDecimal calculateMonthlyLoanInterest(
            LifecycleEventInput input,
            BigDecimal monthlyLoanPayment
    ) {
        if (input == null || input.getNewLoanAmount() == null
                || input.getNewLoanAmount().signum() <= 0
                || input.getLoanInterestRate() == null) {
            return ZERO;
        }
        BigDecimal interest = input.getNewLoanAmount()
                .multiply(input.getLoanInterestRate())
                .divide(new BigDecimal("1200"), 0, java.math.RoundingMode.HALF_UP);
        return interest.min(nvl(monthlyLoanPayment).max(ZERO));
    }

    private BigDecimal calculateExpectedMonthlyLoanPayment(LifecycleEventInput input) {
        if (input == null || input.getNewLoanAmount() == null
                || input.getNewLoanAmount().signum() <= 0
                || input.getLoanInterestRate() == null) {
            return ZERO;
        }
        BigDecimal monthlyInterest = input.getNewLoanAmount()
                .multiply(input.getLoanInterestRate())
                .divide(new BigDecimal("1200"), 0, java.math.RoundingMode.HALF_UP);
        if (input.getEventType() == LifecycleEventType.JEONSE) {
            return monthlyInterest;
        }
        String repaymentType = input.getLoanRepaymentType();
        if ("BULLET".equals(repaymentType)) {
            return monthlyInterest;
        }
        int months = input.getLoanPeriodMonths() != null && input.getLoanPeriodMonths() > 0
                ? input.getLoanPeriodMonths()
                : 360;
        if ("EQUAL_PRINCIPAL".equals(repaymentType)) {
            return input.getNewLoanAmount().divide(
                    BigDecimal.valueOf(months), 0, java.math.RoundingMode.HALF_UP)
                    .add(monthlyInterest);
        }
        double principal = input.getNewLoanAmount().doubleValue();
        double monthlyRate = input.getLoanInterestRate().doubleValue() / 1200.0;
        if (monthlyRate == 0) {
            return input.getNewLoanAmount().divide(
                    BigDecimal.valueOf(months), 0, java.math.RoundingMode.HALF_UP);
        }
        double payment = principal * monthlyRate * Math.pow(1 + monthlyRate, months)
                / (Math.pow(1 + monthlyRate, months) - 1);
        return BigDecimal.valueOf(payment).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private String repaymentTypeLabel(String repaymentType) {
        if ("EQUAL_PRINCIPAL".equals(repaymentType)) return "원금균등상환";
        if ("BULLET".equals(repaymentType)) return "만기일시상환";
        return "원리금균등상환";
    }

    private LifecycleFinancialStateDto findFinalState(
            LifecycleFinancialStateDto initialState,
            List<LifecycleEventResult> eventResults
    ) {
        for (int i = eventResults.size() - 1; i >= 0; i--) {
            LifecycleEventResult result = eventResults.get(i);

            if (result != null && result.getAfterState() != null) {
                return result.getAfterState();
            }
        }

        return initialState;
    }

    private BigDecimal sumEventCost(List<LifecycleEventResult> eventResults) {
        return eventResults.stream()
                .filter(result -> result != null)
                .map(LifecycleEventResult::getEventCost)
                .map(this::nvl)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumSupportBenefit(List<LifecycleEventResult> eventResults) {
        return eventResults.stream()
                .filter(result -> result != null)
                .map(LifecycleEventResult::getSupportBenefit)
                .map(this::nvl)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumFundingShortage(List<LifecycleEventResult> eventResults) {
        return eventResults.stream()
                .filter(result -> result != null)
                .map(LifecycleEventResult::getFundingShortage)
                .map(this::nvl)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal cashAsset(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getCashAsset()) : ZERO;
    }

    private BigDecimal housingAsset(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getHousingAsset()) : ZERO;
    }

    private BigDecimal totalDebt(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getTotalDebt()) : ZERO;
    }

    private BigDecimal monthlySavingCapacity(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getMonthlySavingCapacity()) : ZERO;
    }

    private BigDecimal dsr(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getDsr()) : ZERO;
    }

    private BigDecimal netAsset(LifecycleFinancialStateDto state) {
        return state != null ? nvl(state.getNetAsset()) : ZERO;
    }

    private BigDecimal change(BigDecimal before, BigDecimal after) {
        return nvl(after).subtract(nvl(before));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
