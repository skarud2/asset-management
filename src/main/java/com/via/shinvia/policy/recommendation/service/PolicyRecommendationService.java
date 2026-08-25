package com.via.shinvia.policy.recommendation.service;

import com.via.shinvia.policy.recommendation.asset.service.AssetRecommendationService;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationResultDTO;
import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.mapper.RecommendationUserMapper;
import com.via.shinvia.policy.recommendation.policyloan.service.PolicyLoanRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
// 상품 유형별 추천 결과 통합 기능
public class PolicyRecommendationService {

    private final RecommendationUserMapper recommendationUserMapper;
    private final PolicyLoanRecommendationService policyLoanRecommendationService;
    private final AssetRecommendationService assetRecommendationService;

    public List<RecommendationResultDTO> recommend(Long userId) {
        RecommendationUserDTO user = recommendationUserMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("추천에 필요한 회원 정보를 찾을 수 없습니다.");
        }

        if (user.getResidenceSido() == null || user.getResidenceSido().isBlank()) {
            throw new IllegalArgumentException("맞춤 금융지원상품 설문을 먼저 완료해 주세요.");
        }

        List<RecommendationResultDTO> results = new ArrayList<>();
        results.addAll(policyLoanRecommendationService.recommend(user));
        results.addAll(assetRecommendationService.recommend(user));

        // 추후 SocialFinanceRecommendationService / WelfareRecommendationService를 여기서 합치면 됨
        results.sort(Comparator.comparingInt(RecommendationResultDTO::getMatchScore).reversed());

        return results;
    }
}
