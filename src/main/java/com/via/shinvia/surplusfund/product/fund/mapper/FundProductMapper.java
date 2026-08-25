package com.via.shinvia.surplusfund.product.fund.mapper;

import com.via.shinvia.surplusfund.product.fund.model.FundProduct;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FundProductMapper {
    int upsertCatalog(FundProduct product);

    int upsertDetail(FundProduct product);

    int countFundDetailsByBaseDate(@Param("baseDate") LocalDate baseDate);

    List<FundProduct> findActiveFundProducts();
}
