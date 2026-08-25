package com.via.shinvia.futuresim.mapper;

import com.via.shinvia.futuresim.entity.FuturesimPlanSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PlanSnapshotMapper {

    // 사용자당 이름별로 1행(uk_user_plan_name) — 같은 이름으로 다시 저장하면 덮어쓰고, 새 이름이면 추가된다.
    void upsert(
            @Param("userId") Long userId,
            @Param("planName") String planName,
            @Param("goalAmount") BigDecimal goalAmount,
            @Param("goalPresetKey") String goalPresetKey,
            @Param("benchmarkType") String benchmarkType,
            @Param("benchmarkLabel") String benchmarkLabel,
            @Param("benchmarkMedianNetWorth") BigDecimal benchmarkMedianNetWorth,
            @Param("assumedReturnRate") BigDecimal assumedReturnRate,
            @Param("selectedLeversJson") String selectedLeversJson,
            @Param("baselineMonthsToGoal") int baselineMonthsToGoal,
            @Param("projectedMonthsToGoal") int projectedMonthsToGoal,
            @Param("diffMonths") int diffMonths,
            @Param("loanImpactJson") String loanImpactJson,
            @Param("finalNetWorth") BigDecimal finalNetWorth
    );

    // 목록 화면용 — 사용자가 저장한 계획 전부, 최근 수정순.
    List<FuturesimPlanSnapshot> findAllByUserId(@Param("userId") Long userId);

    // 불러오기용 — 본인 소유인지 userId로 같이 확인한다.
    FuturesimPlanSnapshot findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
