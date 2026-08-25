package com.via.shinvia.lifecycle.reference.service;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;
import com.via.shinvia.lifecycle.reference.dto.LifecycleReferenceDto;
import com.via.shinvia.lifecycle.reference.mapper.LifecycleReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifecycleReferenceService {

    private final LifecycleReferenceMapper lifecycleReferenceMapper;

    public LifecycleReferenceDto getLatestReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String regionSido,
            String regionSigungu
    ) {
        validateKey(eventType, referenceType);
        LifecycleReferenceDto reference = lifecycleReferenceMapper.findLatestReference(
                eventType, referenceType, lifestyleLevel, regionSido, regionSigungu
        );
        if (reference == null) {
            throw missingReference(eventType, referenceType, lifestyleLevel, regionSido, regionSigungu);
        }
        return reference;
    }

    public LifecycleReferenceDto getNationalReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        return getLatestReference(eventType, referenceType, lifestyleLevel, null, null);
    }

    public BigDecimal getRegionalAmount(
            LifecycleEventType eventType,
            String referenceType,
            String regionSido,
            String regionSigungu,
            LifestyleLevel lifestyleLevel
    ) {
        validateKey(eventType, referenceType);
        LifecycleReferenceDto regional = lifecycleReferenceMapper.findLatestReference(
                eventType, referenceType, lifestyleLevel, regionSido, regionSigungu
        );
        if (regional != null && regional.getAmountValue() != null) {
            return regional.getAmountValue();
        }
        return getNationalAmount(eventType, referenceType, lifestyleLevel);
    }

    public BigDecimal getNationalAmount(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference = getNationalReference(eventType, referenceType, lifestyleLevel);
        if (reference.getAmountValue() == null) {
            throw missingValue("amount_value", eventType, referenceType);
        }
        return reference.getAmountValue();
    }

    public BigDecimal getNationalRate(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference = getNationalReference(eventType, referenceType, lifestyleLevel);
        if (reference.getRateValue() == null) {
            throw missingValue("rate_value", eventType, referenceType);
        }
        return reference.getRateValue();
    }

    public BigDecimal getNationalNumeric(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference = getNationalReference(eventType, referenceType, lifestyleLevel);
        if (reference.getNumericValue() == null) {
            throw missingValue("numeric_value", eventType, referenceType);
        }
        return reference.getNumericValue();
    }

    public BigDecimal getVehicleAmount(
            String referenceType,
            VehicleClass vehicleClass,
            VehicleCondition vehicleCondition
    ) {
        if (vehicleClass == null) {
            throw new IllegalArgumentException("차량 차급은 필수입니다.");
        }
        LifecycleReferenceDto reference = lifecycleReferenceMapper.findLatestVehicleReference(
                LifecycleEventType.VEHICLE_PURCHASE,
                referenceType,
                vehicleClass,
                vehicleCondition
        );
        if (reference == null || reference.getAmountValue() == null) {
            throw missingReference(
                    LifecycleEventType.VEHICLE_PURCHASE,
                    referenceType,
                    null,
                    null,
                    null
            );
        }
        return reference.getAmountValue();
    }

    private void validateKey(LifecycleEventType eventType, String referenceType) {
        if (eventType == null) throw new IllegalArgumentException("생애주기 이벤트 유형은 필수입니다.");
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("기준값 유형은 필수입니다.");
        }
    }

    private IllegalStateException missingReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String regionSido,
            String regionSigungu
    ) {
        return new IllegalStateException("필수 생애주기 기준값이 DB에 없습니다: eventType=" + eventType
                + ", referenceType=" + referenceType + ", lifestyleLevel=" + lifestyleLevel
                + ", regionSido=" + regionSido + ", regionSigungu=" + regionSigungu);
    }

    private IllegalStateException missingValue(
            String column,
            LifecycleEventType eventType,
            String referenceType
    ) {
        return new IllegalStateException("생애주기 기준값의 " + column + "이 비어 있습니다: eventType="
                + eventType + ", referenceType=" + referenceType);
    }
}
