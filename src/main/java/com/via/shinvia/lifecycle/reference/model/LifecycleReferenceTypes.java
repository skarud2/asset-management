package com.via.shinvia.lifecycle.reference.model;

import com.via.shinvia.lifecycle.common.model.HousingType;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;

public final class LifecycleReferenceTypes {

    private LifecycleReferenceTypes() {
    }

    // 모든 이벤트 공통
    public static final String LIFESTYLE_COST_MULTIPLIER =
            "LIFESTYLE_COST_MULTIPLIER";

    // 결혼
    public static final String MARRIAGE_TOTAL_COST =
            "TOTAL_COST";

    // 출산
    public static final String POSTPARTUM_CARE_CENTER_COST =
            "POSTPARTUM_CARE_CENTER_COST";

    public static final String POSTPARTUM_HOME_COST =
            "POSTPARTUM_HOME_COST";

    public static final String MONTHLY_CHILDCARE_COST =
            "MONTHLY_CHILDCARE_COST";

    /** 출산 직후 실제 준비물·소모품 기준값 */
    public static final String INITIAL_BABY_ITEM_COST = "INITIAL_BABY_ITEM_COST";
    public static final String INFANT_CAR_SEAT_COST = "INFANT_CAR_SEAT_COST";
    public static final String INFANT_STROLLER_COST = "INFANT_STROLLER_COST";
    public static final String INFANT_CRIB_COST = "INFANT_CRIB_COST";
    public static final String INFANT_OTHER_SETUP_COST = "INFANT_OTHER_SETUP_COST";
    public static final String MONTHLY_DIAPER_COST = "MONTHLY_DIAPER_COST";
    public static final String MONTHLY_FORMULA_COST = "MONTHLY_FORMULA_COST";

    // 차량
    public static final String VEHICLE_BASE_PRICE =
            "VEHICLE_BASE_PRICE";

    public static final String VEHICLE_MONTHLY_MAINTENANCE_COST =
            "VEHICLE_MONTHLY_MAINTENANCE_COST";

    public static final String VEHICLE_LOAN_INTEREST_RATE =
            "VEHICLE_LOAN_INTEREST_RATE";

    public static final String VEHICLE_ACQUISITION_TAX_RATE =
            "VEHICLE_ACQUISITION_TAX_RATE";

    public static final String VEHICLE_LIGHT_ACQUISITION_TAX_RATE =
            "VEHICLE_LIGHT_ACQUISITION_TAX_RATE";

    public static final String VEHICLE_REGISTRATION_FEE =
            "VEHICLE_REGISTRATION_FEE";

    // 월세
    public static final String RENT_BASE_DEPOSIT =
            "RENT_BASE_DEPOSIT";

    public static final String MONTHLY_RENT_BASE_AMOUNT =
            "MONTHLY_RENT_BASE_AMOUNT";

    // 전세
    public static final String JEONSE_BASE_DEPOSIT =
            "JEONSE_BASE_DEPOSIT";

    // 주택 임대차 중개보수 상한 기준
    public static final String RENT_BROKERAGE_RATE_LT_50M = "RENT_BROKERAGE_RATE_LT_50M";
    public static final String RENT_BROKERAGE_CAP_LT_50M = "RENT_BROKERAGE_CAP_LT_50M";
    public static final String RENT_BROKERAGE_RATE_50M_TO_100M = "RENT_BROKERAGE_RATE_50M_TO_100M";
    public static final String RENT_BROKERAGE_CAP_50M_TO_100M = "RENT_BROKERAGE_CAP_50M_TO_100M";
    public static final String RENT_BROKERAGE_RATE_100M_TO_600M = "RENT_BROKERAGE_RATE_100M_TO_600M";
    public static final String RENT_BROKERAGE_RATE_600M_TO_1200M = "RENT_BROKERAGE_RATE_600M_TO_1200M";
    public static final String RENT_BROKERAGE_RATE_1200M_TO_1500M = "RENT_BROKERAGE_RATE_1200M_TO_1500M";
    public static final String RENT_BROKERAGE_RATE_GTE_1500M = "RENT_BROKERAGE_RATE_GTE_1500M";
    public static final String RENT_BROKERAGE_RATE_OFFICETEL = "RENT_BROKERAGE_RATE_OFFICETEL";

    // 주택구매
    public static final String HOME_BASE_PURCHASE_PRICE =
            "HOME_BASE_PURCHASE_PRICE";

    public static final String ACQUISITION_TAX_RATE =
            "ACQUISITION_TAX_RATE";

    public static final String HOME_MONTHLY_MAINTENANCE_COST =
            "HOME_MONTHLY_MAINTENANCE_COST";

    public static final String HOME_LOAN_INTEREST_RATE =
            "HOME_LOAN_INTEREST_RATE";

    // 주거 공통
    public static final String BASE_AREA_SQM =
            "BASE_AREA_SQM";

    // 상환
    public static final String PREPAYMENT_FEE_RATE =
            "PREPAYMENT_FEE_RATE";

    public static String vehicleConditionMultiplier(
            VehicleCondition condition
    ) {
        return "VEHICLE_CONDITION_MULTIPLIER_" + condition.name();
    }

    public static String vehicleClassMultiplier(
            VehicleClass vehicleClass
    ) {
        return "VEHICLE_CLASS_MULTIPLIER_" + vehicleClass.name();
    }

    public static String housingTypeMultiplier(
            HousingType housingType
    ) {
        return "HOUSING_TYPE_MULTIPLIER_" + housingType.name();
    }
}
