package com.via.shinvia.policy.social.repository;

import com.via.shinvia.policy.social.dto.SocialFinanceSearchDTO;
import com.via.shinvia.policy.social.entity.SocialFinanceProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
// 사회연대금융상품 DB 처리
public interface SocialFinanceProductRepository {
    List<SocialFinanceProduct> search(
            @Param("search") SocialFinanceSearchDTO search,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long count(@Param("search") SocialFinanceSearchDTO search);

    Optional<SocialFinanceProduct> findByExternalId(@Param("externalId") String externalId);

    int upsert(SocialFinanceProduct product);

    int deactivateAll();
}
