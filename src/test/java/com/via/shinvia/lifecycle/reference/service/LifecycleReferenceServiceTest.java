package com.via.shinvia.lifecycle.reference.service;

import com.via.shinvia.lifecycle.common.model.HousingType;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;
import com.via.shinvia.lifecycle.reference.dto.LifecycleReferenceDto;
import com.via.shinvia.lifecycle.reference.model.LifecycleReferenceTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "finance.api.service-key=test-key"
})
@Sql(scripts = "/db/lifecycle_reference_init.sql")
class LifecycleReferenceServiceTest {

    @Autowired
    private LifecycleReferenceService lifecycleReferenceService;

    @Test
    void 결혼_기준금액과_배율을_조회한다() {

        LifecycleReferenceDto reference =
                lifecycleReferenceService.getNationalReference(
                        LifecycleEventType.MARRIAGE,
                        LifecycleReferenceTypes.MARRIAGE_TOTAL_COST,
                        null
                );

        BigDecimal multiplier =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.MARRIAGE,
                        LifecycleReferenceTypes.LIFESTYLE_COST_MULTIPLIER,
                        LifestyleLevel.AVERAGE
                );

        assertThat(reference.getAmountValue())
                .isEqualByComparingTo("21390000.00");

        assertThat(reference.getSourceName())
                .isEqualTo("한국소비자원");

        assertThat(multiplier)
                .isEqualByComparingTo("1.000000");
    }

    @Test
    void 출산_산후조리비와_월양육비를_조회한다() {

        BigDecimal postpartumCareCost =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.CHILDBIRTH,
                        LifecycleReferenceTypes.POSTPARTUM_CARE_CENTER_COST,
                        null
                );

        BigDecimal monthlyChildcareCost =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.CHILDBIRTH,
                        LifecycleReferenceTypes.MONTHLY_CHILDCARE_COST,
                        null
                );

        assertThat(postpartumCareCost)
                .isEqualByComparingTo("2865000.00");

        assertThat(monthlyChildcareCost)
                .isEqualByComparingTo("800000.00");
    }

    @Test
    void 차량_기준가격과_선택조건_배율을_조회한다() {

        BigDecimal basePrice =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.VEHICLE_PURCHASE,
                        LifecycleReferenceTypes.VEHICLE_BASE_PRICE,
                        null
                );

        BigDecimal usedMultiplier =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.VEHICLE_PURCHASE,
                        LifecycleReferenceTypes.vehicleConditionMultiplier(
                                VehicleCondition.USED
                        ),
                        null
                );

        BigDecimal suvMultiplier =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.VEHICLE_PURCHASE,
                        LifecycleReferenceTypes.vehicleClassMultiplier(
                                VehicleClass.SUV
                        ),
                        null
                );

        assertThat(basePrice)
                .isEqualByComparingTo("36000000.00");

        assertThat(usedMultiplier)
                .isEqualByComparingTo("0.600000");

        assertThat(suvMultiplier)
                .isEqualByComparingTo("1.150000");
    }

    @Test
    void 월세_보증금과_월세와_기준면적을_조회한다() {

        BigDecimal deposit =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        LifecycleReferenceTypes.RENT_BASE_DEPOSIT,
                        null
                );

        BigDecimal monthlyRent =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        LifecycleReferenceTypes.MONTHLY_RENT_BASE_AMOUNT,
                        null
                );

        BigDecimal baseArea =
                lifecycleReferenceService.getNationalNumeric(
                        LifecycleEventType.MONTHLY_RENT,
                        LifecycleReferenceTypes.BASE_AREA_SQM,
                        null
                );

        assertThat(deposit)
                .isEqualByComparingTo("20000000.00");

        assertThat(monthlyRent)
                .isEqualByComparingTo("700000.00");

        assertThat(baseArea)
                .isEqualByComparingTo("59.0000");
    }

    @Test
    void 전세_보증금과_주거형태_배율을_조회한다() {

        BigDecimal deposit =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.JEONSE,
                        LifecycleReferenceTypes.JEONSE_BASE_DEPOSIT,
                        null
                );

        BigDecimal apartmentMultiplier =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.JEONSE,
                        LifecycleReferenceTypes.housingTypeMultiplier(
                                HousingType.APARTMENT
                        ),
                        null
                );

        assertThat(deposit)
                .isEqualByComparingTo("300000000.00");

        assertThat(apartmentMultiplier)
                .isEqualByComparingTo("1.150000");
    }

    @Test
    void 주택구매_가격과_취득세율을_조회한다() {

        BigDecimal purchasePrice =
                lifecycleReferenceService.getNationalAmount(
                        LifecycleEventType.HOME_PURCHASE,
                        LifecycleReferenceTypes.HOME_BASE_PURCHASE_PRICE,
                        null
                );

        BigDecimal acquisitionTaxRate =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.HOME_PURCHASE,
                        LifecycleReferenceTypes.ACQUISITION_TAX_RATE,
                        null
                );

        BigDecimal baseArea =
                lifecycleReferenceService.getNationalNumeric(
                        LifecycleEventType.HOME_PURCHASE,
                        LifecycleReferenceTypes.BASE_AREA_SQM,
                        null
                );

        assertThat(purchasePrice)
                .isEqualByComparingTo("600000000.00");

        assertThat(acquisitionTaxRate)
                .isEqualByComparingTo("0.011000");

        assertThat(baseArea)
                .isEqualByComparingTo("84.0000");
    }

    @Test
    void 대출상환_중도상환수수료율을_조회한다() {

        BigDecimal feeRate =
                lifecycleReferenceService.getNationalRate(
                        LifecycleEventType.REPAYMENT,
                        LifecycleReferenceTypes.PREPAYMENT_FEE_RATE,
                        null
                );

        assertThat(feeRate)
                .isEqualByComparingTo("0.006500");
    }
}