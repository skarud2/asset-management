package com.via.shinvia.policy.recommendation.profile.mapper;

import com.via.shinvia.policy.recommendation.profile.dto.PolicyRecommendationProfileDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 정책상품 추천 설문 DB 접근 기능
public interface PolicyRecommendationProfileMapper {
    PolicyRecommendationProfileDTO findByUserId(@Param("userId") Long userId);

    int upsertProfile(PolicyRecommendationProfileDTO profile);
}
