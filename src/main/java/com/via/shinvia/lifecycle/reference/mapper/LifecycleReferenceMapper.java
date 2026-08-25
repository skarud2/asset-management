package com.via.shinvia.lifecycle.reference.mapper;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;
import com.via.shinvia.lifecycle.reference.dto.LifecycleReferenceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifecycleReferenceMapper {

    /**
     * 조건에 맞는 가장 최신 활성 기준값 1건 조회
     */
    LifecycleReferenceDto findLatestReference(
            @Param("eventType")
            LifecycleEventType eventType,

            @Param("referenceType")
            String referenceType,

            @Param("lifestyleLevel")
            LifestyleLevel lifestyleLevel,

            @Param("regionSido")
            String regionSido,

            @Param("regionSigungu")
            String regionSigungu
    );

    LifecycleReferenceDto findLatestVehicleReference(
            @Param("eventType") LifecycleEventType eventType,
            @Param("referenceType") String referenceType,
            @Param("vehicleClass") VehicleClass vehicleClass,
            @Param("vehicleCondition") VehicleCondition vehicleCondition
    );
}
