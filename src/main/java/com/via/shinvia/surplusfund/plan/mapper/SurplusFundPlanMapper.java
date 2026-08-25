package com.via.shinvia.surplusfund.plan.mapper;

import com.via.shinvia.surplusfund.allocation.dto.AssetAllocationResponse;
import com.via.shinvia.surplusfund.plan.model.SurplusFundPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurplusFundPlanMapper {

    // 운용계획 1건 저장
    int insertPlan(SurplusFundPlan plan);

    // 운용계획에 연결된 CASH/ETF/FUND 배분 결과 저장
    int insertAllocations(
            @Param("planId") Long planId,
            @Param("allocations") List<AssetAllocationResponse> allocations
    );

    int insertReason(
            @Param("planId") Long planId,
            @Param("reasonOrder") int reasonOrder,
            @Param("reasonText") String reasonText
    );
}
