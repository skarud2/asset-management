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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleEventInputAssemblerServiceTest {

    @Mock
    private LifecycleSurveyService surveyService;

    @Mock
    private LifecycleReferenceService referenceService;

    @Mock
    private LifecycleProductService productService;

    @Mock
    private LifecycleWelfareService welfareService;

    @InjectMocks
    private LifecycleEventInputAssemblerService assemblerService;

    @Test
    void assembleMarriageBuildsInput() {
        MarriageSurveyResponse survey = MarriageSurveyResponse.builder()
                .lifecycleEventId(1L)
                .eventOrder(1)
                .targetDate(LocalDate.of(2027, 5, 1))
                .lifestyleLevel(LifestyleLevel.AVERAGE)
                .userContributionRate(new BigDecimal("0.50"))
                .familySupportAmount(new BigDecimal("3000000"))
                .build();

        LifecycleSupportDto support = LifecycleSupportDto.builder()
                .supportName("신혼부부 지원")
                .effectType(SupportEffectType.CASH_INFLOW)
                .amount(new BigDecimal("1000000"))
                .recommendationStatus("ELIGIBLE")
                .build();

        when(surveyService.getMarriageSurvey(1L)).thenReturn(survey);
        when(welfareService.getSupports(
                eq(LifecycleEventType.MARRIAGE),
                isNull(),
                isNull(),
                eq(10L)
        )).thenReturn(List.of(support));
        when(referenceService.getNationalAmount(
                eq(LifecycleEventType.MARRIAGE),
                eq("TOTAL_COST"),
                isNull()
        )).thenReturn(new BigDecimal("20000000"));
        when(referenceService.getNationalRate(
                eq(LifecycleEventType.MARRIAGE),
                eq("LIFESTYLE_COST_MULTIPLIER"),
                eq(LifestyleLevel.AVERAGE)
        )).thenReturn(BigDecimal.ONE);
        when(referenceService.getRegionalAmount(
                LifecycleEventType.MARRIAGE,
                "MEAL_COST_PER_GUEST",
                null,
                null,
                null
        )).thenReturn(new BigDecimal("50000"));
        when(productService.getRecommendedProducts(
                anyLong(),
                anyString(),
                eq(LifecycleEventType.MARRIAGE),
                any(),
                anyInt()
        )).thenReturn(List.of(product()));

        LifecycleEventInput input = assemblerService.assembleEvent(
                10L,
                "user@example.com",
                LifecycleEventType.MARRIAGE,
                1L
        );

        assertEquals(LifecycleEventType.MARRIAGE, input.getEventType());
        assertEquals(new BigDecimal("30000000.00"), input.getEstimatedCost());
        assertEquals(new BigDecimal("12000000.00"), input.getUserRequiredAmount());
        assertEquals(new BigDecimal("20000000.00"), input.getMarriageHallCost());
        assertEquals(new BigDecimal("10000000.00"), input.getMarriageMealCost());
        assertEquals(new BigDecimal("0.00"), input.getCashInflowAmount());
        assertEquals(new BigDecimal("3000000.00"), input.getFamilySupportAmount());
        assertEquals(1, input.getSupports().size());
        assertEquals(1, input.getRecommendedProducts().size());
    }

    @Test
    void assembleChildbirthBuildsMonthlyExpense() {
        ChildbirthSurveyResponse survey = ChildbirthSurveyResponse.builder()
                .lifecycleEventId(2L)
                .eventOrder(2)
                .targetDate(LocalDate.of(2028, 1, 1))
                .lifestyleLevel(LifestyleLevel.AVERAGE)
                .postpartumCare(true)
                .regionSido("서울특별시")
                .regionSigungu("강남구")
                .build();

        LifecycleSupportDto monthlySupport = LifecycleSupportDto.builder()
                .supportName("아동수당")
                .effectType(SupportEffectType.MONTHLY_CASH_INFLOW)
                .amount(new BigDecimal("100000"))
                .recommendationStatus("ELIGIBLE")
                .build();

        when(surveyService.getChildbirthSurvey(2L)).thenReturn(survey);
        when(welfareService.getSupports(
                eq(LifecycleEventType.CHILDBIRTH),
                eq("서울특별시"),
                eq("강남구"),
                eq(10L)
        )).thenReturn(List.of(monthlySupport));
        when(referenceService.getNationalAmount(
                eq(LifecycleEventType.CHILDBIRTH),
                eq("POSTPARTUM_CARE_CENTER_COST"),
                isNull()
        )).thenReturn(new BigDecimal("3000000"));
        when(referenceService.getNationalAmount(
                eq(LifecycleEventType.CHILDBIRTH),
                eq("MONTHLY_CHILDCARE_COST"),
                isNull()
        )).thenReturn(new BigDecimal("300000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("INFANT_CAR_SEAT_COST"), isNull()))
                .thenReturn(new BigDecimal("300000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("INFANT_STROLLER_COST"), isNull()))
                .thenReturn(new BigDecimal("500000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("INFANT_CRIB_COST"), isNull()))
                .thenReturn(new BigDecimal("300000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("INFANT_OTHER_SETUP_COST"), isNull()))
                .thenReturn(new BigDecimal("400000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("MONTHLY_DIAPER_COST"), isNull()))
                .thenReturn(new BigDecimal("120000"));
        when(referenceService.getNationalAmount(eq(LifecycleEventType.CHILDBIRTH), eq("MONTHLY_FORMULA_COST"), isNull()))
                .thenReturn(new BigDecimal("180000"));
        when(productService.getRecommendedProducts(
                anyLong(),
                anyString(),
                eq(LifecycleEventType.CHILDBIRTH)
        )).thenReturn(List.of());

        LifecycleEventInput input = assemblerService.assembleEvent(
                10L,
                "user@example.com",
                LifecycleEventType.CHILDBIRTH,
                2L
        );

        assertEquals(new BigDecimal("4500000.00"), input.getEstimatedCost());
        assertEquals(new BigDecimal("4500000.00"), input.getUserRequiredAmount());
        assertEquals(new BigDecimal("600000.00"), input.getAdditionalMonthlyExpense());
    }

    @Test
    void assembleVehicleBuildsLoanAndAsset() {
        VehicleSurveyResponse survey = VehicleSurveyResponse.builder()
                .lifecycleEventId(3L)
                .eventOrder(3)
                .targetDate(LocalDate.of(2027, 8, 1))
                .cashPaymentAmount(new BigDecimal("10000000"))
                .loanAmount(null)
                .loanPeriodMonths(60)
                .build();

        when(surveyService.getVehicleSurvey(3L)).thenReturn(survey);
        when(referenceService.getNationalAmount(
                eq(LifecycleEventType.VEHICLE_PURCHASE),
                eq("VEHICLE_BASE_PRICE"),
                isNull()
        )).thenReturn(new BigDecimal("30000000"));
        when(referenceService.getVehicleAmount(
                eq("VEHICLE_MONTHLY_MAINTENANCE_COST"),
                eq(VehicleClass.MIDSIZE),
                isNull()
        )).thenReturn(new BigDecimal("460000"));
        when(referenceService.getNationalRate(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_ACQUISITION_TAX_RATE",
                null
        )).thenReturn(new BigDecimal("0.07"));
        when(referenceService.getNationalAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_REGISTRATION_FEE",
                null
        )).thenReturn(new BigDecimal("2000"));
        when(referenceService.getNationalRate(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_LOAN_INTEREST_RATE",
                null
        )).thenReturn(new BigDecimal("0.05"));
        when(productService.getRecommendedProducts(
                anyLong(),
                anyString(),
                eq(LifecycleEventType.VEHICLE_PURCHASE),
                any(),
                eq(60)
        )).thenReturn(List.of(product()));

        LifecycleEventInput input = assemblerService.assembleEvent(
                10L,
                "user@example.com",
                LifecycleEventType.VEHICLE_PURCHASE,
                3L
        );

        assertEquals(new BigDecimal("32102000.00"), input.getEstimatedCost());
        assertEquals(new BigDecimal("12102000.00"), input.getUserRequiredAmount());
        assertEquals(new BigDecimal("20000000.00"), input.getNewLoanAmount());
        assertEquals(new BigDecimal("30000000.00"), input.getAcquiredAssetAmount());
        assertEquals(new BigDecimal("460000.00"), input.getAdditionalMonthlyExpense());
    }

    @Test
    void assembleVehiclePrefersSurveyValuesOverReferences() {
        VehicleSurveyResponse survey = VehicleSurveyResponse.builder()
                .lifecycleEventId(30L)
                .eventOrder(1)
                .targetDate(LocalDate.of(2029, 7, 1))
                .vehiclePrice(new BigDecimal("11111111"))
                .cashPaymentAmount(new BigDecimal("11111110"))
                .loanAmount(BigDecimal.ONE)
                .loanPeriodMonths(24)
                .monthlyMaintenanceCost(new BigDecimal("11111"))
                .build();

        when(surveyService.getVehicleSurvey(30L)).thenReturn(survey);
        when(referenceService.getNationalRate(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_ACQUISITION_TAX_RATE",
                null
        )).thenReturn(new BigDecimal("0.07"));
        when(referenceService.getNationalAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_REGISTRATION_FEE",
                null
        )).thenReturn(new BigDecimal("2000"));
        when(referenceService.getNationalRate(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_LOAN_INTEREST_RATE",
                null
        )).thenReturn(new BigDecimal("0.05"));
        when(productService.getRecommendedProducts(
                10L,
                "user@example.com",
                LifecycleEventType.VEHICLE_PURCHASE,
                BigDecimal.ONE,
                24
        )).thenReturn(List.of());

        LifecycleEventInput input = assemblerService.assembleEvent(
                10L,
                "user@example.com",
                LifecycleEventType.VEHICLE_PURCHASE,
                30L
        );

        assertEquals(new BigDecimal("11890888.77"), input.getEstimatedCost());
        assertEquals(new BigDecimal("1.00"), input.getNewLoanAmount());
        assertEquals(new BigDecimal("11111.00"), input.getAdditionalMonthlyExpense());
        verify(referenceService, never()).getNationalAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                "VEHICLE_BASE_PRICE",
                null
        );
        verify(referenceService, never()).getVehicleAmount(
                "VEHICLE_MONTHLY_MAINTENANCE_COST",
                VehicleClass.MIDSIZE,
                null
        );
    }

    @Test
    void assembleJeonseBuildsDepositLoanAndAsset() {
        JeonseSurveyResponse survey = JeonseSurveyResponse.builder()
                .lifecycleEventId(4L)
                .eventOrder(4)
                .targetDate(LocalDate.of(2027, 10, 1))
                .lifestyleLevel(LifestyleLevel.AVERAGE)
                .housingType("APARTMENT")
                .desiredArea(new BigDecimal("59"))
                .ownFundAmount(new BigDecimal("100000000"))
                .desiredLoanAmount(null)
                .regionSido("서울특별시")
                .regionSigungu("강남구")
                .build();

        when(surveyService.getJeonseSurvey(4L)).thenReturn(survey);
        when(welfareService.getSupports(
                eq(LifecycleEventType.JEONSE),
                eq("서울특별시"),
                eq("강남구"),
                eq(10L)
        )).thenReturn(List.of());
        when(referenceService.getNationalAmount(
                eq(LifecycleEventType.JEONSE),
                eq("JEONSE_BASE_DEPOSIT"),
                isNull()
        )).thenReturn(new BigDecimal("300000000"));
        when(referenceService.getNationalRate(
                eq(LifecycleEventType.JEONSE),
                eq("LIFESTYLE_COST_MULTIPLIER"),
                eq(LifestyleLevel.AVERAGE)
        )).thenReturn(BigDecimal.ONE);
        when(referenceService.getNationalRate(
                eq(LifecycleEventType.JEONSE),
                eq("HOUSING_TYPE_MULTIPLIER_APARTMENT"),
                isNull()
        )).thenReturn(BigDecimal.ONE);
        when(referenceService.getNationalNumeric(
                eq(LifecycleEventType.JEONSE),
                eq("BASE_AREA_SQM"),
                isNull()
        )).thenReturn(new BigDecimal("59"));
        when(productService.getRecommendedProducts(
                anyLong(),
                anyString(),
                eq(LifecycleEventType.JEONSE),
                any(),
                eq(24)
        )).thenReturn(List.of(product()));

        LifecycleEventInput input = assemblerService.assembleEvent(
                10L,
                "user@example.com",
                LifecycleEventType.JEONSE,
                4L
        );

        assertEquals(new BigDecimal("300000000.00"), input.getEstimatedCost());
        assertEquals(new BigDecimal("200000000.00"), input.getNewLoanAmount());
        assertEquals(new BigDecimal("100000000.00"), input.getUserRequiredAmount());
        assertEquals(new BigDecimal("300000000.00"), input.getAcquiredAssetAmount());
    }

    private LifecycleProductDto product() {
        return LifecycleProductDto.builder()
                .productId(1L)
                .productType("TEST")
                .productName("추천상품")
                .build();
    }
}
