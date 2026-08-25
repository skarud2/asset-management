package com.via.shinvia.policy.recommendation.asset.mapper;

import com.via.shinvia.policy.recommendation.asset.dto.AssetFormationProductDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 자산형성 추천 상품 조회 기능
public interface AssetRecommendationMapper {
    List<AssetFormationProductDTO> findActiveProducts();
}
