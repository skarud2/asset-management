package com.via.shinvia.policy.recommendation.policyloan.mapper;

import com.via.shinvia.policy.recommendation.policyloan.dto.PolicySupportProductDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 정책대출 추천 상품 조회 기능
public interface PolicyLoanRecommendationMapper {
    List<PolicySupportProductDTO> findActiveProducts();
}
