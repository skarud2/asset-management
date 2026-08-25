package com.via.shinvia.surplusfund.product.etf.mapper;

import com.via.shinvia.surplusfund.product.etf.model.EtfProduct;
import com.via.shinvia.surplusfund.product.etf.model.EtfProductSort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EtfProductMapper {

    int upsertCatalog(EtfProduct product);

    int upsertDetail(EtfProduct product);

    List<EtfProduct> findProducts(
            @Param("keyword") String keyword,
            @Param("sort") EtfProductSort sort,
            @Param("limit") int limit
    );

    EtfProduct findById(@Param("investmentProductId") Long investmentProductId);

    LocalDate findLatestBaseDate();
}

