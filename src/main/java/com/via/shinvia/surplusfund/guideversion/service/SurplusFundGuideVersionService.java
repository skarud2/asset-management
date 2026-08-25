package com.via.shinvia.surplusfund.guideversion.service;

import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionCreateRequest;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionCreateResponse;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionDetailResponse;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionSummaryResponse;
import com.via.shinvia.surplusfund.guideversion.exception.GuideVersionConflictException;
import com.via.shinvia.surplusfund.guideversion.mapper.SurplusFundGuideVersionMapper;
import com.via.shinvia.surplusfund.guideversion.model.GuideCalculationSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuidePlanSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionAllocation;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionEtfSnapshot;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionReason;
import com.via.shinvia.surplusfund.guideversion.model.GuideVersionRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class SurplusFundGuideVersionService {

    private static final int MAX_ETF_COUNT = 4;
    private static final String SNAPSHOT_SCHEMA_VERSION = "SURPLUS_GUIDE_V1.0";
    private static final BigDecimal TOTAL_ALLOCATION_RATIO = new BigDecimal("100.00");

    private final SurplusFundGuideVersionMapper guideVersionMapper;

    public SurplusFundGuideVersionService(
            SurplusFundGuideVersionMapper guideVersionMapper
    ) {
        this.guideVersionMapper = guideVersionMapper;
    }

    @Transactional
    public GuideVersionCreateResponse create(
            Long userId,
            GuideVersionCreateRequest request
    ) {
        requireUserId(userId);

        String idempotencyKey = request.idempotencyKey().trim();
        Long lockedUserId = guideVersionMapper.lockUserById(userId);
        if (lockedUserId == null) {
            throw new NoSuchElementException("사용자를 찾을 수 없습니다.");
        }

        GuideVersionRecord existing = guideVersionMapper.findByIdempotencyKey(
                userId,
                idempotencyKey
        );
        if (existing != null) {
            return toCreateResponse(existing);
        }

        GuideVersionRecord existingPlan = guideVersionMapper.findByPlanIdAndUserId(
                request.surplusFundPlanId(),
                userId
        );
        if (existingPlan != null) {
            throw new GuideVersionConflictException("이미 저장이 완료된 운용 기록입니다.");
        }

        GuideCalculationSnapshot calculation = guideVersionMapper.findCalculationByIdAndUserId(
                request.surplusFundCalculationId(),
                userId
        );
        if (calculation == null) {
            throw new NoSuchElementException("해당 사용자의 여유자금 계산 결과를 찾을 수 없습니다.");
        }

        GuidePlanSnapshot plan = guideVersionMapper.findPlanByIdAndUserId(
                request.surplusFundPlanId(),
                userId
        );
        if (plan == null) {
            throw new NoSuchElementException("해당 사용자의 자산배분 결과를 찾을 수 없습니다.");
        }

        validateCalculationAndPlan(calculation, plan);

        List<GuideVersionAllocation> allocations = guideVersionMapper.findAllocationsByPlanId(
                plan.getSurplusFundPlanId()
        );
        validateAllocations(plan, allocations);

        List<GuideVersionReason> reasons = guideVersionMapper.findReasonsByPlanId(
                plan.getSurplusFundPlanId()
        );
        if (reasons == null || reasons.isEmpty()) {
            throw new GuideVersionConflictException(
                    "판정 이유가 저장되지 않은 운용계획입니다. 설문 분석을 다시 실행해주세요."
            );
        }

        List<Long> selectedEtfProductIds = List.copyOf(request.selectedEtfProductIds());
        List<GuideVersionEtfSnapshot> etfSnapshots = loadEtfSnapshotsInSelectionOrder(
                selectedEtfProductIds
        );

        LocalDateTime completedAt = LocalDateTime.now();
        GuideVersionRecord guideVersion = new GuideVersionRecord();
        guideVersion.setUserId(userId);
        guideVersion.setGuideVersionNo(guideVersionMapper.findNextVersionNo(userId));
        guideVersion.setGuideName(resolveGuideName(request.guideName(), plan, completedAt));
        guideVersion.setSurplusFundCalculationId(calculation.getSurplusFundCalculationId());
        guideVersion.setSurplusFundPlanId(plan.getSurplusFundPlanId());
        guideVersion.setSelectedEtfCount(etfSnapshots.size());
        guideVersion.setSnapshotSchemaVersion(SNAPSHOT_SCHEMA_VERSION);
        guideVersion.setIdempotencyKey(idempotencyKey);
        guideVersion.setCompletedAt(completedAt);

        int insertedGuideCount = guideVersionMapper.insertGuideVersion(guideVersion);
        if (insertedGuideCount != 1 || guideVersion.getSurplusFundGuideVersionId() == null) {
            throw new IllegalStateException("여유자금 운용 기록 저장에 실패했습니다.");
        }

        if (!etfSnapshots.isEmpty()) {
            int insertedEtfCount = guideVersionMapper.insertEtfSnapshots(
                    guideVersion.getSurplusFundGuideVersionId(),
                    etfSnapshots
            );
            if (insertedEtfCount != etfSnapshots.size()) {
                throw new IllegalStateException("관심 ETF 스냅샷 저장에 실패했습니다.");
            }
        }

        return toCreateResponse(guideVersion);
    }

    @Transactional(readOnly = true)
    public List<GuideVersionSummaryResponse> findAll(Long userId) {
        requireUserId(userId);

        return guideVersionMapper.findSummariesByUserId(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuideVersionDetailResponse findDetail(Long userId, Long guideVersionId) {
        requireUserId(userId);
        if (guideVersionId == null || guideVersionId <= 0) {
            throw new IllegalArgumentException("운용 기록 ID가 올바르지 않습니다.");
        }

        GuideVersionRecord guideVersion = guideVersionMapper.findByIdAndUserId(
                guideVersionId,
                userId
        );
        if (guideVersion == null) {
            throw new NoSuchElementException("운용 기록을 찾을 수 없습니다.");
        }

        GuideCalculationSnapshot calculation = guideVersionMapper.findCalculationByIdAndUserId(
                guideVersion.getSurplusFundCalculationId(),
                userId
        );
        GuidePlanSnapshot plan = guideVersionMapper.findPlanByIdAndUserId(
                guideVersion.getSurplusFundPlanId(),
                userId
        );
        if (calculation == null || plan == null) {
            throw new IllegalStateException("운용 기록의 원본 계산 또는 자산배분 결과가 없습니다.");
        }

        List<GuideVersionAllocation> allocations = guideVersionMapper.findAllocationsByPlanId(
                plan.getSurplusFundPlanId()
        );
        List<GuideVersionReason> reasons = guideVersionMapper.findReasonsByPlanId(
                plan.getSurplusFundPlanId()
        );
        List<GuideVersionEtfSnapshot> etfs = guideVersionMapper.findEtfSnapshotsByGuideVersionId(
                guideVersionId
        );

        return toDetailResponse(guideVersion, calculation, plan, allocations, reasons, etfs);
    }

    @Transactional
    public GuideVersionCreateResponse rename(
            Long userId,
            Long guideVersionId,
            String guideName
    ) {
        requireUserId(userId);
        if (guideVersionId == null || guideVersionId <= 0) {
            throw new IllegalArgumentException("운용 기록 ID가 올바르지 않습니다.");
        }

        String normalizedName = guideName == null ? "" : guideName.trim();
        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("운용 기록 이름은 1자 이상 100자 이하여야 합니다.");
        }

        int updatedCount = guideVersionMapper.updateGuideName(
                guideVersionId,
                userId,
                normalizedName
        );
        if (updatedCount != 1) {
            throw new NoSuchElementException("이름을 변경할 운용 기록을 찾을 수 없습니다.");
        }

        GuideVersionRecord updated = guideVersionMapper.findByIdAndUserId(
                guideVersionId,
                userId
        );
        return toCreateResponse(updated);
    }

    private void validateCalculationAndPlan(
            GuideCalculationSnapshot calculation,
            GuidePlanSnapshot plan
    ) {
        if (calculation.getFinalSurplusAmount() == null
                || plan.getOperationAmount() == null
                || calculation.getFinalSurplusAmount().compareTo(plan.getOperationAmount()) != 0) {
            throw new GuideVersionConflictException(
                    "여유자금 계산 금액과 자산배분 운용금액이 일치하지 않습니다. 1단계부터 다시 진행해주세요."
            );
        }
    }

    private void validateAllocations(
            GuidePlanSnapshot plan,
            List<GuideVersionAllocation> allocations
    ) {
        if (allocations == null || allocations.isEmpty()) {
            throw new GuideVersionConflictException("저장된 자산배분 결과가 없습니다.");
        }

        BigDecimal ratioTotal = allocations.stream()
                .map(GuideVersionAllocation::getAllocationRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ratioTotal.compareTo(TOTAL_ALLOCATION_RATIO) != 0) {
            throw new GuideVersionConflictException("자산배분 비율 합계가 100%가 아닙니다.");
        }

        BigDecimal amountTotal = allocations.stream()
                .map(GuideVersionAllocation::getAllocationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amountTotal.compareTo(plan.getOperationAmount()) != 0) {
            throw new GuideVersionConflictException("자산배분 금액 합계가 운용금액과 일치하지 않습니다.");
        }
    }

    private List<GuideVersionEtfSnapshot> loadEtfSnapshotsInSelectionOrder(
            List<Long> productIds
    ) {
        if (productIds.size() > MAX_ETF_COUNT) {
            throw new IllegalArgumentException("관심 ETF는 최대 4개까지 선택할 수 있습니다.");
        }

        Set<Long> uniqueProductIds = new HashSet<>(productIds);
        if (uniqueProductIds.size() != productIds.size()) {
            throw new IllegalArgumentException("동일한 ETF 상품을 중복 선택할 수 없습니다.");
        }
        if (productIds.isEmpty()) {
            return List.of();
        }

        List<GuideVersionEtfSnapshot> sources = guideVersionMapper.findEtfSourcesByIds(productIds);
        Map<Long, GuideVersionEtfSnapshot> sourceById = new HashMap<>();
        for (GuideVersionEtfSnapshot source : sources) {
            sourceById.put(source.getSourceInvestmentProductId(), source);
        }

        if (sourceById.size() != productIds.size()) {
            throw new NoSuchElementException(
                    "선택한 ETF 중 현재 조회할 수 없는 상품이 있습니다. 상품을 다시 선택해주세요."
            );
        }

        for (int index = 0; index < productIds.size(); index++) {
            GuideVersionEtfSnapshot snapshot = sourceById.get(productIds.get(index));
            snapshot.setSelectionOrder(index + 1);
        }

        return productIds.stream()
                .map(sourceById::get)
                .toList();
    }

    private String resolveGuideName(
            String requestedName,
            GuidePlanSnapshot plan,
            LocalDateTime completedAt
    ) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        String date = String.format(
                Locale.KOREA,
                "%04d.%02d.%02d",
                completedAt.getYear(),
                completedAt.getMonthValue(),
                completedAt.getDayOfMonth()
        );
        String styleName = switch (plan.getInvestmentStyle()) {
            case "STABLE" -> "안정형";
            case "BALANCED" -> "균형형";
            case "AGGRESSIVE" -> "공격형";
            default -> plan.getInvestmentStyle();
        };
        String formattedAmount = NumberFormat.getIntegerInstance(Locale.KOREA)
                .format(plan.getOperationAmount());

        return date + " " + styleName + " · " + formattedAmount + "원";
    }

    private GuideVersionCreateResponse toCreateResponse(GuideVersionRecord record) {
        return new GuideVersionCreateResponse(
                record.getSurplusFundGuideVersionId(),
                record.getGuideVersionNo(),
                record.getGuideName(),
                record.getSelectedEtfCount(),
                record.getCompletedAt()
        );
    }

    private GuideVersionSummaryResponse toSummaryResponse(GuideVersionRecord record) {
        return new GuideVersionSummaryResponse(
                record.getSurplusFundGuideVersionId(),
                record.getGuideVersionNo(),
                record.getGuideName(),
                record.getOperationAmount(),
                record.getInvestmentStyle(),
                record.getSelectedEtfCount(),
                record.getCompletedAt()
        );
    }

    private GuideVersionDetailResponse toDetailResponse(
            GuideVersionRecord guideVersion,
            GuideCalculationSnapshot calculation,
            GuidePlanSnapshot plan,
            List<GuideVersionAllocation> allocations,
            List<GuideVersionReason> reasons,
            List<GuideVersionEtfSnapshot> etfs
    ) {
        GuideVersionDetailResponse.Calculation calculationResponse =
                new GuideVersionDetailResponse.Calculation(
                        calculation.getSurplusFundCalculationId(),
                        calculation.getTotalCurrentBalance(),
                        calculation.getSelectedAccountBalance(),
                        calculation.getEstimatedNextIncomeDate(),
                        calculation.getAdjustedNextIncomeDate(),
                        calculation.getEstimatedLivingExpense(),
                        calculation.getAdjustedLivingExpense(),
                        calculation.getEstimatedScheduledExpense(),
                        calculation.getAdjustedScheduledExpense(),
                        calculation.getRecommendedEmergencyFund(),
                        calculation.getAdjustedEmergencyFund(),
                        calculation.getCalculatedSurplusAmount(),
                        calculation.getFinalSurplusAmount()
                );

        List<GuideVersionDetailResponse.Allocation> allocationResponses = allocations.stream()
                .map(allocation -> new GuideVersionDetailResponse.Allocation(
                        allocation.getAssetType(),
                        allocation.getAllocationRatio(),
                        allocation.getAllocationAmount()
                ))
                .toList();

        GuideVersionDetailResponse.PlanResult planResponse =
                new GuideVersionDetailResponse.PlanResult(
                        plan.getSurplusFundPlanId(),
                        plan.getOperationAmount(),
                        plan.getInvestmentStyle(),
                        plan.getRuleVersion(),
                        reasons.stream().map(GuideVersionReason::getReasonText).toList(),
                        allocationResponses
                );

        List<GuideVersionDetailResponse.EtfSnapshot> etfResponses = etfs.stream()
                .map(etf -> new GuideVersionDetailResponse.EtfSnapshot(
                        etf.getSelectionOrder(),
                        etf.getSourceInvestmentProductId(),
                        etf.getProductCode(),
                        etf.getIsinCode(),
                        etf.getProductName(),
                        etf.getProviderName(),
                        etf.getCategory(),
                        etf.getPriceBaseDate(),
                        etf.getClosingPrice(),
                        etf.getPreviousDayChange(),
                        etf.getFluctuationRate(),
                        etf.getNav(),
                        etf.getOpeningPrice(),
                        etf.getHighPrice(),
                        etf.getLowPrice(),
                        etf.getTradingVolume(),
                        etf.getTradingValue(),
                        etf.getListedShareCount(),
                        etf.getMarketCap(),
                        etf.getNetAssetTotalAmount(),
                        etf.getBaseIndexName(),
                        etf.getBaseIndexClose(),
                        etf.getProductLastSyncedAt(),
                        etf.getCapturedAt()
                ))
                .toList();

        return new GuideVersionDetailResponse(
                guideVersion.getSurplusFundGuideVersionId(),
                guideVersion.getGuideVersionNo(),
                guideVersion.getGuideName(),
                guideVersion.getSnapshotSchemaVersion(),
                guideVersion.getCompletedAt(),
                calculationResponse,
                planResponse,
                etfResponses
        );
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("로그인 사용자 ID가 필요합니다.");
        }
    }
}
