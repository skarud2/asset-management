package com.via.shinvia.policy.asset.repository;

import com.via.shinvia.policy.asset.dto.AssetProductSearchDTO;
import com.via.shinvia.policy.asset.entity.AssetFormationProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
// 자산형성상품 DB 처리
public interface AssetFormationProductRepository {
    List<AssetFormationProduct> search(
            @Param("search") AssetProductSearchDTO search,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long count(@Param("search") AssetProductSearchDTO search);

    Optional<AssetFormationProduct> findByExternalId(@Param("externalId") String externalId);

    int upsert(AssetFormationProduct product);

    int deactivateAll();
}
