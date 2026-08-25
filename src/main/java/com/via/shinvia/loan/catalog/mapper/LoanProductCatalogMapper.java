package com.via.shinvia.loan.catalog.mapper;

import com.via.shinvia.loan.catalog.dto.command.LoanProductCatalogModels;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoanProductCatalogMapper {

    int deactivateByLoanType(@Param("loanType") String loanType);

    int upsertCatalog(LoanProductCatalogModels.CatalogProduct catalog);

    Long findCatalogProductId(
            @Param("sourceType") String sourceType,
            @Param("sourceProductKey") String sourceProductKey,
            @Param("sourceProductSubtype") String sourceProductSubtype
    );

    int upsertHousingDetail(
            @Param("catalogProductId") Long catalogProductId,
            @Param("detail") LoanProductCatalogModels.HousingDetail detail
    );

    int deleteHousingOptions(@Param("catalogProductId") Long catalogProductId);

    int insertHousingOption(
            @Param("catalogProductId") Long catalogProductId,
            @Param("option") LoanProductCatalogModels.HousingOption option
    );

    int upsertCreditDetail(
            @Param("catalogProductId") Long catalogProductId,
            @Param("detail") LoanProductCatalogModels.CreditDetail detail
    );

    int deleteCreditOptions(@Param("catalogProductId") Long catalogProductId);

    int insertCreditOption(
            @Param("catalogProductId") Long catalogProductId,
            @Param("option") LoanProductCatalogModels.CreditOption option
    );

    List<Map<String, Object>> findCatalogSummaries(
            @Param("loanType") String loanType,
            @Param("active") Boolean active
    );

    Map<String, Object> findCatalogSummaryById(
            @Param("catalogProductId") Long catalogProductId
    );

    Map<String, Object> findHousingDetail(
            @Param("catalogProductId") Long catalogProductId
    );

    List<Map<String, Object>> findHousingOptions(
            @Param("catalogProductId") Long catalogProductId
    );

    Map<String, Object> findCreditDetail(
            @Param("catalogProductId") Long catalogProductId
    );

    List<Map<String, Object>> findCreditOptions(
            @Param("catalogProductId") Long catalogProductId
    );
}
