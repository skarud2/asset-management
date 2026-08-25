package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.recommendation.service.LifecycleProductService;
import com.via.shinvia.lifecycle.recommendation.service.LifecycleWelfareService;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import com.via.shinvia.lifecycle.survey.dto.*;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.via.shinvia.lifecycle.reference.model.LifecycleReferenceTypes.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LifecycleEventInputAssemblerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final String LIFESTYLE_COST_MULTIPLIER =
            "LIFESTYLE_COST_MULTIPLIER";

    private static final String TOTAL_COST = "TOTAL_COST";
    private static final String POSTPARTUM_CARE_CENTER_COST =
            "POSTPARTUM_CARE_CENTER_COST";
    private static final String POSTPARTUM_HOME_COST =
            "POSTPARTUM_HOME_COST";
    private static final String MONTHLY_CHILDCARE_COST =
            "MONTHLY_CHILDCARE_COST";
    private static final String INITIAL_BABY_ITEM_COST = "INITIAL_BABY_ITEM_COST";
    private static final String INFANT_CAR_SEAT_COST = "INFANT_CAR_SEAT_COST";
    private static final String INFANT_STROLLER_COST = "INFANT_STROLLER_COST";
    private static final String INFANT_CRIB_COST = "INFANT_CRIB_COST";
    private static final String INFANT_OTHER_SETUP_COST = "INFANT_OTHER_SETUP_COST";
    private static final String MONTHLY_DIAPER_COST = "MONTHLY_DIAPER_COST";
    private static final String MONTHLY_FORMULA_COST = "MONTHLY_FORMULA_COST";

    private static final String VEHICLE_BASE_PRICE =
            "VEHICLE_BASE_PRICE";
    private static final String VEHICLE_MONTHLY_MAINTENANCE_COST =
            "VEHICLE_MONTHLY_MAINTENANCE_COST";
    private static final String VEHICLE_LOAN_INTEREST_RATE =
            "VEHICLE_LOAN_INTEREST_RATE";

    private static final String RENT_BASE_DEPOSIT =
            "RENT_BASE_DEPOSIT";
    private static final String MONTHLY_RENT_BASE_AMOUNT =
            "MONTHLY_RENT_BASE_AMOUNT";

    private static final String JEONSE_BASE_DEPOSIT =
            "JEONSE_BASE_DEPOSIT";

    private static final String HOME_BASE_PURCHASE_PRICE =
            "HOME_BASE_PURCHASE_PRICE";
    private static final String ACQUISITION_TAX_RATE =
            "ACQUISITION_TAX_RATE";
    private static final String BASE_AREA_SQM = "BASE_AREA_SQM";

    private final LifecycleSurveyService surveyService;
    private final LifecycleReferenceService referenceService;
    private final LifecycleProductService productService;
    private final LifecycleWelfareService welfareService;

    public List<LifecycleEventInput> assembleScenario(
            Long userId,
            String loginEmail,
            Long scenarioId
    ) {
        return surveyService.getTimelineEvents(scenarioId)
                .stream()
                .filter(event -> event.getEventType() != LifecycleEventType.REPAYMENT)
                .map(event -> assembleEvent(
                        userId,
                        loginEmail,
                        event.getEventType(),
                        event.getEventId()
                ))
                .toList();
    }

    public LifecycleEventInput assembleEvent(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType,
            Long lifecycleEventId
    ) {
        return switch (eventType) {
            case MARRIAGE ->
                    assembleMarriage(userId, loginEmail, lifecycleEventId);
            case CHILDBIRTH ->
                    assembleChildbirth(userId, loginEmail, lifecycleEventId);
            case VEHICLE_PURCHASE ->
                    assembleVehicle(userId, loginEmail, lifecycleEventId);
            case MONTHLY_RENT ->
                    assembleMonthlyRent(userId, loginEmail, lifecycleEventId);
            case JEONSE ->
                    assembleJeonse(userId, loginEmail, lifecycleEventId);
            case HOME_PURCHASE ->
                    assembleHomePurchase(userId, loginEmail, lifecycleEventId);
            case REPAYMENT ->
                    assembleRepayment(userId, loginEmail, lifecycleEventId);
        };
    }

    private LifecycleEventInput assembleMarriage(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        MarriageSurveyResponse survey =
                surveyService.getMarriageSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.MARRIAGE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu(),
                        userId
                );

        boolean customMarriageCost = survey.getCustomEstimatedCost() != null
                && survey.getCustomEstimatedCost().signum() > 0;
        BigDecimal marriageMultiplier = lifestyleMultiplier(LifecycleEventType.MARRIAGE, survey.getLifestyleLevel());
        BigDecimal hallCost = customMarriageCost ? ZERO
                : referenceAmount(LifecycleEventType.MARRIAGE, TOTAL_COST).multiply(marriageMultiplier);
        BigDecimal mealCost = customMarriageCost ? ZERO
                : referenceService.getRegionalAmount(
                    LifecycleEventType.MARRIAGE, "MEAL_COST_PER_GUEST",
                    survey.getRegionSido(), survey.getRegionSigungu(), null)
                    .multiply(BigDecimal.valueOf(survey.getGuestCount() != null ? survey.getGuestCount() : 200))
                    .multiply(marriageMultiplier);
        BigDecimal furnitureCost = !customMarriageCost && Boolean.TRUE.equals(survey.getFurnitureIncluded())
                ? referenceAmount(LifecycleEventType.MARRIAGE, "FURNITURE_COST") : ZERO;
        BigDecimal honeymoonCost = !customMarriageCost && Boolean.TRUE.equals(survey.getHoneymoonIncluded())
                ? referenceAmount(LifecycleEventType.MARRIAGE, "HONEYMOON_COST") : ZERO;
        BigDecimal estimatedCost = customMarriageCost
                ? survey.getCustomEstimatedCost()
                : hallCost.add(mealCost).add(furnitureCost).add(honeymoonCost);

        BigDecimal userShare = estimatedCost.multiply(
                defaultIfNull(survey.getUserContributionRate(), ONE)
        );

        BigDecimal familySupport = nvl(survey.getFamilySupportAmount());
        BigDecimal cashInflow = sumSupportAmount(
                supports,
                SupportEffectType.CASH_INFLOW
        );

        BigDecimal userRequiredAmount = maxZero(
                userShare.subtract(familySupport).subtract(cashInflow)
        );

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.MARRIAGE,
                        userRequiredAmount,
                        36
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.MARRIAGE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(estimatedCost))
                .userRequiredAmount(money(userRequiredAmount))
                .userContributionAmount(money(userShare))
                .additionalMonthlyExpense(ZERO)
                .cashInflowAmount(money(cashInflow))
                .familySupportAmount(money(familySupport))
                .marriageHallCost(money(hallCost))
                .marriageMealCost(money(mealCost))
                .marriageFurnitureCost(money(furnitureCost))
                .marriageHoneymoonCost(money(honeymoonCost))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleChildbirth(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        ChildbirthSurveyResponse survey =
                surveyService.getChildbirthSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.CHILDBIRTH,
                        survey.getRegionSido(),
                        survey.getRegionSigungu(),
                        userId
                );

        BigDecimal postpartumCareCost = Boolean.TRUE.equals(survey.getPostpartumCare())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, POSTPARTUM_CARE_CENTER_COST)
                : referenceAmount(LifecycleEventType.CHILDBIRTH, POSTPARTUM_HOME_COST);

        // 양육 수준 배율 대신 출산 직후 실제 준비물(카시트·유모차·침대 등) 기준값을 합산한다.
        boolean firstChild = survey.getChildOrder() == null || survey.getChildOrder() <= 1;
        BigDecimal carSeatCost = firstChild || Boolean.TRUE.equals(survey.getRepurchaseCarSeat())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, INFANT_CAR_SEAT_COST) : ZERO;
        BigDecimal strollerCost = firstChild || Boolean.TRUE.equals(survey.getRepurchaseStroller())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, INFANT_STROLLER_COST) : ZERO;
        BigDecimal cribCost = firstChild || Boolean.TRUE.equals(survey.getRepurchaseCrib())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, INFANT_CRIB_COST) : ZERO;
        BigDecimal otherSetupCost = firstChild || Boolean.TRUE.equals(survey.getRepurchaseOtherSetup())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, INFANT_OTHER_SETUP_COST) : ZERO;
        BigDecimal initialCost = postpartumCareCost.add(carSeatCost).add(strollerCost).add(cribCost).add(otherSetupCost);

        BigDecimal monthlyChildcareCost =
                referenceAmount(LifecycleEventType.CHILDBIRTH, MONTHLY_CHILDCARE_COST)
                        .add(referenceAmount(LifecycleEventType.CHILDBIRTH, MONTHLY_DIAPER_COST))
                        .add(referenceAmount(LifecycleEventType.CHILDBIRTH, MONTHLY_FORMULA_COST));

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal monthlySupport =
                sumSupportAmount(supports, SupportEffectType.MONTHLY_CASH_INFLOW);

        BigDecimal additionalMonthlyExpense =
                maxZero(monthlyChildcareCost.subtract(monthlySupport));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.CHILDBIRTH
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.CHILDBIRTH,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .childOrder(survey.getChildOrder())
                .repurchaseCarSeat(survey.getRepurchaseCarSeat())
                .repurchaseStroller(survey.getRepurchaseStroller())
                .repurchaseCrib(survey.getRepurchaseCrib())
                .repurchaseOtherSetup(survey.getRepurchaseOtherSetup())
                .postpartumCare(survey.getPostpartumCare())
                .childbirthRegionSido(survey.getRegionSido())
                .childbirthRegionSigungu(survey.getRegionSigungu())
                .postpartumCareCost(money(postpartumCareCost))
                .infantCarSeatCost(money(carSeatCost))
                .infantStrollerCost(money(strollerCost))
                .infantCribCost(money(cribCost))
                .infantOtherSetupCost(money(otherSetupCost))
                .estimatedCost(money(initialCost))
                .userRequiredAmount(money(maxZero(initialCost.subtract(cashInflow))))
                .additionalMonthlyExpense(money(additionalMonthlyExpense))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleVehicle(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        VehicleSurveyResponse survey =
                surveyService.getVehicleSurvey(lifecycleEventId);

        BigDecimal estimatedPrice = survey.getVehiclePrice() != null
                && survey.getVehiclePrice().signum() > 0
                ? survey.getVehiclePrice()
                : calculateVehiclePrice(survey.getVehicleModel(), survey.getVehicleCondition());

        BigDecimal newLoanAmount = nvl(survey.getLoanAmount());

        if (newLoanAmount.signum() == 0
                && survey.getCashPaymentAmount() != null) {
            newLoanAmount = maxZero(
                    estimatedPrice.subtract(survey.getCashPaymentAmount())
            );
        }

        BigDecimal userRequiredAmount =
                maxZero(estimatedPrice.subtract(newLoanAmount));

        BigDecimal acquisitionTaxRate = referenceRate(
                LifecycleEventType.VEHICLE_PURCHASE,
                "RAY".equalsIgnoreCase(survey.getVehicleModel())
                        ? VEHICLE_LIGHT_ACQUISITION_TAX_RATE
                        : VEHICLE_ACQUISITION_TAX_RATE
        );
        BigDecimal acquisitionTax = estimatedPrice.multiply(nvl(acquisitionTaxRate));
        BigDecimal registrationFee = nvl(referenceAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                VEHICLE_REGISTRATION_FEE
        ));
        BigDecimal transactionCost = acquisitionTax.add(registrationFee);
        BigDecimal totalCost = estimatedPrice.add(transactionCost);
        userRequiredAmount = userRequiredAmount.add(transactionCost);

        BigDecimal monthlyMaintenance = survey.getMonthlyMaintenanceCost() != null
                && survey.getMonthlyMaintenanceCost().signum() > 0
                ? survey.getMonthlyMaintenanceCost()
                : calculateVehicleMonthlyCost(survey.getVehicleModel());

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.VEHICLE_PURCHASE,
                        positiveOrNull(newLoanAmount),
                        survey.getLoanPeriodMonths()
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.VEHICLE_PURCHASE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                null)
                .estimatedCost(money(totalCost))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(monthlyMaintenance))
                .cashInflowAmount(ZERO)
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(estimatedPrice))
                .taxAmount(money(acquisitionTax))
                .registrationFeeAmount(money(registrationFee))
                .loanInterestRate(newLoanAmount.signum() > 0
                        ? referenceRate(LifecycleEventType.VEHICLE_PURCHASE, VEHICLE_LOAN_INTEREST_RATE)
                        : null)
                .supports(List.of())
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleMonthlyRent(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        MonthlyRentSurveyResponse survey =
                surveyService.getMonthlyRentSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.MONTHLY_RENT,
                        survey.getRegionSido(),
                        survey.getRegionSigungu(),
                        userId
                );

        BigDecimal deposit = survey.getDesiredDeposit() != null && survey.getDesiredDeposit().signum() > 0
                ? survey.getDesiredDeposit()
                : calculateHousingAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        RENT_BASE_DEPOSIT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                );

        BigDecimal monthlyRent = survey.getDesiredMonthlyRent() != null && survey.getDesiredMonthlyRent().signum() > 0
                ? survey.getDesiredMonthlyRent()
                : calculateHousingAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        MONTHLY_RENT_BASE_AMOUNT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                );

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal monthlySupport =
                sumSupportAmount(supports, SupportEffectType.MONTHLY_CASH_INFLOW);

        BigDecimal monthlyExpense = monthlyRent
                .add(nvl(survey.getMonthlyManagementFee()))
                .subtract(monthlySupport);

        BigDecimal userRequiredAmount =
                maxZero(deposit.subtract(cashInflow));
        BigDecimal brokerageFee = calculateRentalBrokerageFee(
                deposit,
                monthlyRent,
                survey.getHousingType(),
                LifecycleEventType.MONTHLY_RENT
        );
        userRequiredAmount = userRequiredAmount.add(brokerageFee);

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.MONTHLY_RENT,
                        positiveOrNull(userRequiredAmount),
                        36
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.MONTHLY_RENT,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(deposit.add(brokerageFee)))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(maxZero(monthlyExpense)))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(money(deposit))
                .brokerageFeeAmount(money(brokerageFee))
                .keepExistingHome(survey.getKeepExistingHome())
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleJeonse(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        JeonseSurveyResponse survey =
                surveyService.getJeonseSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.JEONSE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu(),
                        userId
                );

        BigDecimal deposit = survey.getDesiredJeonseAmount() != null && survey.getDesiredJeonseAmount().signum() > 0
                ? survey.getDesiredJeonseAmount()
                : calculateHousingAmount(
                        LifecycleEventType.JEONSE,
                        JEONSE_BASE_DEPOSIT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                );

        BigDecimal newLoanAmount = nvl(survey.getDesiredLoanAmount());

        if (newLoanAmount.signum() == 0
                && survey.getOwnFundAmount() != null) {
            newLoanAmount = maxZero(
                    deposit.subtract(survey.getOwnFundAmount())
            );
        }

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(deposit.subtract(newLoanAmount).subtract(cashInflow));
        BigDecimal brokerageFee = calculateRentalBrokerageFee(
                deposit,
                ZERO,
                survey.getHousingType(),
                LifecycleEventType.JEONSE
        );
        userRequiredAmount = userRequiredAmount.add(brokerageFee);

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.JEONSE,
                        positiveOrNull(newLoanAmount),
                        24
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.JEONSE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(deposit.add(brokerageFee)))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(ZERO)
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(deposit))
                .brokerageFeeAmount(money(brokerageFee))
                .keepExistingHome(survey.getKeepExistingHome())
                .loanPeriodMonths(24)
                .loanInterestRate(new BigDecimal("3.8"))
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleHomePurchase(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        HomePurchaseSurveyResponse survey =
                surveyService.getHomePurchaseSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.HOME_PURCHASE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu(),
                        userId
                );

        BigDecimal purchasePrice = survey.getDesiredPurchasePrice() != null && survey.getDesiredPurchasePrice().signum() > 0
                ? survey.getDesiredPurchasePrice()
                : calculateHousingAmount(
                        LifecycleEventType.HOME_PURCHASE,
                        HOME_BASE_PURCHASE_PRICE,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                );

        BigDecimal acquisitionTax = purchasePrice.multiply(
                referenceRate(
                        LifecycleEventType.HOME_PURCHASE,
                        ACQUISITION_TAX_RATE
                )
        );

        BigDecimal brokerageFee = calculateHomePurchaseBrokerageFee(
                purchasePrice,
                survey.getHousingType()
        );

        BigDecimal totalCost = purchasePrice.add(acquisitionTax).add(brokerageFee);

        BigDecimal newLoanAmount = survey.getOwnFundAmount() == null
                ? ZERO
                : maxZero(purchasePrice.subtract(survey.getOwnFundAmount()));

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(totalCost.subtract(newLoanAmount).subtract(cashInflow));

        BigDecimal recommendationAmount = newLoanAmount.signum() > 0
                ? newLoanAmount
                : purchasePrice;
        BigDecimal monthlyMaintenanceCost = referenceAmount(
                LifecycleEventType.HOME_PURCHASE,
                HOME_MONTHLY_MAINTENANCE_COST
        );
        BigDecimal mortgageInterestRate = referenceRate(
                LifecycleEventType.HOME_PURCHASE,
                HOME_LOAN_INTEREST_RATE
        );

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.HOME_PURCHASE,
                        positiveOrNull(recommendationAmount),
                        survey.getLoanPeriodMonths()
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.HOME_PURCHASE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(totalCost))
                .userRequiredAmount(money(userRequiredAmount))
                .userContributionAmount(money(nvl(survey.getOwnFundAmount())))
                .additionalMonthlyExpense(money(monthlyMaintenanceCost))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(purchasePrice))
                .taxAmount(money(acquisitionTax))
                .brokerageFeeAmount(money(brokerageFee))
                .loanPeriodMonths(survey.getLoanPeriodMonths())
                .loanInterestRate(mortgageInterestRate)
                .loanRepaymentType(survey.getRepaymentType())
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private BigDecimal calculateHomePurchaseBrokerageFee(
            BigDecimal purchasePrice,
            String housingType
    ) {
        BigDecimal price = nvl(purchasePrice);
        if (price.signum() <= 0) return ZERO;

        // 주거용 오피스텔 매매 상한요율 0.5%. 그 외 주택은 거래금액 구간별 상한요율을 적용한다.
        if ("OFFICETEL".equalsIgnoreCase(housingType)) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_OFFICETEL"));
        }
        if (price.compareTo(new BigDecimal("50000000")) < 0) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_LT_50M"))
                    .min(referenceAmount(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_CAP_LT_50M"));
        }
        if (price.compareTo(new BigDecimal("200000000")) < 0) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_50M_TO_200M"))
                    .min(referenceAmount(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_CAP_50M_TO_200M"));
        }
        if (price.compareTo(new BigDecimal("900000000")) < 0) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_200M_TO_900M"));
        }
        if (price.compareTo(new BigDecimal("1200000000")) < 0) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_900M_TO_1200M"));
        }
        if (price.compareTo(new BigDecimal("1500000000")) < 0) {
            return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_1200M_TO_1500M"));
        }
        return price.multiply(referenceRate(LifecycleEventType.HOME_PURCHASE, "BROKERAGE_RATE_GTE_1500M"));
    }

    private BigDecimal calculateRentalBrokerageFee(
            BigDecimal deposit,
            BigDecimal monthlyRent,
            String housingType,
            LifecycleEventType eventType
    ) {
        BigDecimal transactionAmount = nvl(deposit).add(nvl(monthlyRent).multiply(BigDecimal.valueOf(100)));
        if (nvl(monthlyRent).signum() > 0
                && transactionAmount.compareTo(new BigDecimal("50000000")) < 0) {
            transactionAmount = nvl(deposit).add(nvl(monthlyRent).multiply(BigDecimal.valueOf(70)));
        }
        if ("OFFICETEL".equalsIgnoreCase(housingType)) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_OFFICETEL)));
        }
        if (transactionAmount.compareTo(new BigDecimal("50000000")) < 0) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_LT_50M)))
                    .min(nvl(referenceAmount(eventType, RENT_BROKERAGE_CAP_LT_50M)));
        }
        if (transactionAmount.compareTo(new BigDecimal("100000000")) < 0) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_50M_TO_100M)))
                    .min(nvl(referenceAmount(eventType, RENT_BROKERAGE_CAP_50M_TO_100M)));
        }
        if (transactionAmount.compareTo(new BigDecimal("600000000")) < 0) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_100M_TO_600M)));
        }
        if (transactionAmount.compareTo(new BigDecimal("1200000000")) < 0) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_600M_TO_1200M)));
        }
        if (transactionAmount.compareTo(new BigDecimal("1500000000")) < 0) {
            return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_1200M_TO_1500M)));
        }
        return transactionAmount.multiply(nvl(referenceRate(eventType, RENT_BROKERAGE_RATE_GTE_1500M)));
    }

    private LifecycleEventInput assembleRepayment(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        RepaymentSurveyResponse survey =
                surveyService.getRepaymentSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.REPAYMENT,
                        null,
                        null,
                        userId
                );

        BigDecimal repaymentAmount = nvl(survey.getRepaymentAmount());
        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(repaymentAmount.subtract(cashInflow));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.REPAYMENT,
                        positiveOrNull(repaymentAmount),
                        60
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.REPAYMENT,
                survey.getEventOrder(),
                survey.getTargetDate(),
                null)
                .estimatedCost(money(repaymentAmount))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(nvl(
                        survey.getAdditionalMonthlyRepayment()
                )))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .targetLoanAccountId(survey.getLoanAccountId())
                .repaymentAction(survey.getRepaymentAction())
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private BigDecimal calculateVehiclePrice(String vehicleModel, String vehicleCondition) {
        if (vehicleModel == null || vehicleModel.isBlank()) {
            return referenceAmount(LifecycleEventType.VEHICLE_PURCHASE, VEHICLE_BASE_PRICE);
        }
        String condition = "USED".equalsIgnoreCase(vehicleCondition) ? "USED" : "NEW";
        return referenceAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_MODEL_PRICE_" + vehicleModel.toUpperCase() + "_" + condition
        );
    }

    private BigDecimal calculateVehicleMonthlyCost(String vehicleModel) {
        return referenceService.getVehicleAmount(
                VEHICLE_MONTHLY_MAINTENANCE_COST,
                vehicleClassForModel(vehicleModel),
                null
        );
    }

    private VehicleClass vehicleClassForModel(String vehicleModel) {
        if (vehicleModel == null) {
            return VehicleClass.MIDSIZE;
        }
        return switch (vehicleModel.toUpperCase()) {
            case "RAY" -> VehicleClass.COMPACT;
            case "K3", "AVANTE" -> VehicleClass.SEMI_MIDSIZE;
            case "G70" -> VehicleClass.MIDSIZE;
            case "G80", "G90" -> VehicleClass.LARGE;
            case "GV60", "GV70", "GV80" -> VehicleClass.SUV;
            default -> VehicleClass.MIDSIZE;
        };
    }

    private BigDecimal calculateHousingAmount(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String housingType,
            BigDecimal desiredArea
    ) {
        BigDecimal amount = referenceAmount(eventType, referenceType)
                .multiply(lifestyleMultiplier(eventType, lifestyleLevel))
                .multiply(housingTypeMultiplier(eventType, housingType));

        BigDecimal baseArea = referenceNumeric(eventType, BASE_AREA_SQM);

        if (desiredArea != null
                && desiredArea.signum() > 0
                && baseArea.signum() > 0) {
            amount = amount.multiply(
                    desiredArea.divide(baseArea, 6, RoundingMode.HALF_UP)
            );
        }

        return amount;
    }

    private LifecycleEventInput.LifecycleEventInputBuilder baseBuilder(
            Long lifecycleEventId,
            LifecycleEventType eventType,
            Integer eventOrder,
            java.time.LocalDate targetDate,
            LifestyleLevel lifestyleLevel
    ) {
        return LifecycleEventInput.builder()
                .lifecycleEventId(lifecycleEventId)
                .eventType(eventType)
                .eventOrder(eventOrder)
                .targetDate(targetDate)
                .lifestyleLevel(lifestyleLevel);
    }

    private BigDecimal lifestyleMultiplier(
            LifecycleEventType eventType,
            LifestyleLevel lifestyleLevel
    ) {
        if (lifestyleLevel == null
                || lifestyleLevel == LifestyleLevel.CUSTOM) {
            return ONE;
        }

        return referenceService.getNationalRate(
                eventType,
                LIFESTYLE_COST_MULTIPLIER,
                lifestyleLevel
        );
    }

    private BigDecimal housingTypeMultiplier(
            LifecycleEventType eventType,
            String housingType
    ) {
        if (housingType == null || housingType.isBlank()) {
            return ONE;
        }

        return referenceRate(
                eventType,
                "HOUSING_TYPE_MULTIPLIER_" + housingType
        );
    }

    private BigDecimal referenceAmount(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalAmount(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal referenceRate(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalRate(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal referenceNumeric(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalNumeric(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal sumSupportAmount(
            List<LifecycleSupportDto> supports,
            SupportEffectType effectType
    ) {
        if (supports == null || supports.isEmpty()) {
            return ZERO;
        }

        return supports.stream()
                .filter(support -> "ELIGIBLE".equals(
                        support.getRecommendationStatus()
                ))
                .filter(support -> support.getEffectType() == effectType)
                .filter(support -> "CONFIRMED".equalsIgnoreCase(support.getRecommendationStatus()))
                .map(LifecycleSupportDto::getAmount)
                .filter(amount -> amount != null)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal positiveOrDefault(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        if (value != null && value.signum() > 0) {
            return value;
        }

        return defaultValue;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }

        return value;
    }

    private BigDecimal defaultIfNull(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal maxZero(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return ZERO;
        }

        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }
}
