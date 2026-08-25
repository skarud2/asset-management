package com.via.shinvia.surplusfund.preference.service;

import com.via.shinvia.surplusfund.allocation.dto.AssetAllocationResponse;
import com.via.shinvia.surplusfund.allocation.service.AssetAllocationService;
import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceRequest;
import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceResponse;
import com.via.shinvia.surplusfund.plan.mapper.SurplusFundPlanMapper;
import com.via.shinvia.surplusfund.plan.model.SurplusFundPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
public class InvestmentPreferenceService {

    public static final String GUIDE_NOTICE =
            "본 결과는 교육용 모의 운용 가이드이며 실제 금융상품 투자권유·자문이나 수익 보장을 의미하지 않습니다.";

    private final InvestmentStyleClassifier investmentStyleClassifier;
    private final AssetAllocationService assetAllocationService;
    private final SurplusFundPlanMapper surplusFundPlanMapper;

    public InvestmentPreferenceService(
            InvestmentStyleClassifier investmentStyleClassifier,
            AssetAllocationService assetAllocationService,
            SurplusFundPlanMapper surplusFundPlanMapper
    ) {
        this.investmentStyleClassifier = investmentStyleClassifier;
        this.assetAllocationService = assetAllocationService;
        this.surplusFundPlanMapper = surplusFundPlanMapper;
    }

    @Transactional
    public InvestmentPreferenceResponse analyzeAndSave(
            Long userId,
            InvestmentPreferenceRequest request
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인 사용자 ID가 필요합니다.");
        }

        // request 설문 입력 -> ClassificationResult (성향, 점수, 판정 근거) - > 자산 배분 계산 -> Response (최종 API 응답)
        InvestmentStyleClassifier.ClassificationResult classification = investmentStyleClassifier.classify(request);

        List<AssetAllocationResponse> allocations = assetAllocationService.allocate(
                request.operationAmount(),
                classification.investmentStyle()
        );

        SurplusFundPlan plan = new SurplusFundPlan(
                userId,
                request.operationAmount().setScale(2, RoundingMode.HALF_UP),
                request.investmentPurpose(),
                request.investmentPeriodMonths(),
                request.lossToleranceLevel(),
                request.liquidityNeed(),
                request.experienceLevel(),
                Boolean.TRUE.equals(request.surplusAmountConfirmed()),
                Boolean.TRUE.equals(request.guideNoticeConfirmed()),
                classification.investmentStyle(),
                classification.ruleVersion()
        );

        int insertedPlanCount = surplusFundPlanMapper.insertPlan(plan);
        if (insertedPlanCount != 1 || plan.getSurplusFundPlanId() == null) {
            throw new IllegalStateException("여유자금 운용계획 저장에 실패했습니다.");
        }

        int insertedAllocationCount = surplusFundPlanMapper.insertAllocations(
                plan.getSurplusFundPlanId(),
                allocations
        );
        if (insertedAllocationCount != allocations.size()) {
            throw new IllegalStateException("자산배분 결과 저장에 실패했습니다.");
        }

        for (int index = 0; index < classification.reasons().size(); index++) {
            int insertedReasonCount = surplusFundPlanMapper.insertReason(
                    plan.getSurplusFundPlanId(),
                    index + 1,
                    classification.reasons().get(index)
            );
            if (insertedReasonCount != 1) {
                throw new IllegalStateException("운용성향 판정 이유 저장에 실패했습니다.");
            }
        }

        // 결과 응답 만들기
        return new InvestmentPreferenceResponse(
                plan.getSurplusFundPlanId(),
                classification.investmentStyle(),
                classification.ruleVersion(),
                classification.score(),
                classification.reasons(),
                allocations,
                GUIDE_NOTICE
        );
    }
}
