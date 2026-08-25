package com.via.shinvia.policy.recommendation.profile.service;

import com.via.shinvia.policy.recommendation.profile.dto.PolicyRecommendationProfileDTO;
import com.via.shinvia.policy.recommendation.profile.mapper.PolicyRecommendationProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
// 정책상품 추천 설문 조회·저장 기능
public class PolicyRecommendationProfileService {
    private final PolicyRecommendationProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public PolicyRecommendationProfileDTO getProfile(Long userId) {
        return profileMapper.findByUserId(userId);
    }

    @Transactional
    public void saveProfile(PolicyRecommendationProfileDTO profile) {
        validateIncomeInformation(profile);
        profileMapper.upsertProfile(profile);
    }

    private void validateIncomeInformation(PolicyRecommendationProfileDTO profile) {
        if (Boolean.FALSE.equals(profile.getHasIncome())) {
            profile.setIncomeVerifiable(null);
            return;
        }

        if (Boolean.TRUE.equals(profile.getHasIncome())
                && !"YES".equals(profile.getIncomeVerifiable())
                && !"NO".equals(profile.getIncomeVerifiable())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "소득이 있는 경우 소득증빙 가능 여부를 선택해야 합니다."
            );
        }
    }
}
