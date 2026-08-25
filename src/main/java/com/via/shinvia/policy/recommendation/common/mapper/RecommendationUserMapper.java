package com.via.shinvia.policy.recommendation.common.mapper;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 추천 판정에 사용할 회원·금융프로필·설문 정보 조회 기능
public interface RecommendationUserMapper {
    RecommendationUserDTO findByUserId(@Param("userId") Long userId);
}
