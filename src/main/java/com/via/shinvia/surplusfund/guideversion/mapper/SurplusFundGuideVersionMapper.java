package com.via.shinvia.surplusfund.guideversion.mapper;

import com.via.shinvia.surplusfund.guideversion.model.GuideCalculationSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuidePlanSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionAllocation;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionEtfSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionReason;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurplusFundGuideVersionMapper {

    Long lockUserById(@Param("userId") Long userId);

    GuideVersionRecord findByIdempotencyKey(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    GuideVersionRecord findByPlanIdAndUserId(
            @Param("planId") Long planId,
            @Param("userId") Long userId
    );

    GuideCalculationSnapshot findCalculationByIdAndUserId(
            @Param("calculationId") Long calculationId,
            @Param("userId") Long userId
    );

    GuidePlanSnapshot findPlanByIdAndUserId(
            @Param("planId") Long planId,
            @Param("userId") Long userId
    );

    List<GuideVersionAllocation> findAllocationsByPlanId(
            @Param("planId") Long planId
    );

    List<GuideVersionReason> findReasonsByPlanId(
            @Param("planId") Long planId
    );

    List<GuideVersionEtfSnapshot> findEtfSourcesByIds(
            @Param("productIds") List<Long> productIds
    );

    int findNextVersionNo(@Param("userId") Long userId);

    int insertGuideVersion(GuideVersionRecord guideVersion);

    int insertEtfSnapshots(
            @Param("guideVersionId") Long guideVersionId,
            @Param("snapshots") List<GuideVersionEtfSnapshot> snapshots
    );

    List<GuideVersionRecord> findSummariesByUserId(
            @Param("userId") Long userId
    );

    GuideVersionRecord findByIdAndUserId(
            @Param("guideVersionId") Long guideVersionId,
            @Param("userId") Long userId
    );

    List<GuideVersionEtfSnapshot> findEtfSnapshotsByGuideVersionId(
            @Param("guideVersionId") Long guideVersionId
    );

    int updateGuideName(
            @Param("guideVersionId") Long guideVersionId,
            @Param("userId") Long userId,
            @Param("guideName") String guideName
    );
}
