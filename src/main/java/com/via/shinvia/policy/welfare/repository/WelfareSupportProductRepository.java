package com.via.shinvia.policy.welfare.repository;

import com.via.shinvia.policy.welfare.dto.WelfareSupportSearchDTO;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
// 복합지원상품 DB 처리
public interface WelfareSupportProductRepository {
    List<WelfareSupportProduct> search(
            @Param("search") WelfareSupportSearchDTO search,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long count(@Param("search") WelfareSupportSearchDTO search);

    Optional<WelfareSupportProduct> findByExternalId(@Param("externalId") String externalId);

    int upsert(WelfareSupportProduct product);

    int deactivateAll();

    int deactivateBySourceType(@Param("sourceType") String sourceType);

    List<WelfareSupportProduct> findLifecycleCandidates(
            @Param("keywords") List<String> keywords,
            @Param("regionSido") String regionSido,
            @Param("regionSigungu") String regionSigungu
    );
}
