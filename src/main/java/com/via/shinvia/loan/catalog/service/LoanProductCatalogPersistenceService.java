package com.via.shinvia.loan.catalog.service;

import com.via.shinvia.loan.catalog.dto.command.LoanProductCatalogModels;
import com.via.shinvia.loan.catalog.mapper.LoanProductCatalogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductCatalogPersistenceService {

    private final LoanProductCatalogMapper mapper;

    @Transactional
    public SaveCount replaceHousingCatalog(
            String loanType,
            List<LoanProductCatalogModels.HousingProduct> products
    ) {
        mapper.deactivateByLoanType(loanType);

        int optionCount = 0;
        for (LoanProductCatalogModels.HousingProduct normalized : products) {
            mapper.upsertCatalog(normalized.catalog());
            Long catalogProductId = resolveCatalogProductId(normalized.catalog());

            mapper.upsertHousingDetail(catalogProductId, normalized.detail());
            mapper.deleteHousingOptions(catalogProductId);

            for (LoanProductCatalogModels.HousingOption option : normalized.options()) {
                mapper.insertHousingOption(catalogProductId, option);
                optionCount++;
            }
        }
        return new SaveCount(products.size(), optionCount);
    }

    @Transactional
    public SaveCount replaceCreditCatalog(
            List<LoanProductCatalogModels.CreditProduct> products
    ) {
        mapper.deactivateByLoanType("CREDIT");

        int optionCount = 0;
        for (LoanProductCatalogModels.CreditProduct normalized : products) {
            mapper.upsertCatalog(normalized.catalog());
            Long catalogProductId = resolveCatalogProductId(normalized.catalog());

            mapper.upsertCreditDetail(catalogProductId, normalized.detail());
            mapper.deleteCreditOptions(catalogProductId);

            for (LoanProductCatalogModels.CreditOption option : normalized.options()) {
                mapper.insertCreditOption(catalogProductId, option);
                optionCount++;
            }
        }
        return new SaveCount(products.size(), optionCount);
    }

    private Long resolveCatalogProductId(LoanProductCatalogModels.CatalogProduct catalog) {
        Long id = mapper.findCatalogProductId(
                catalog.sourceType(),
                catalog.sourceProductKey(),
                catalog.sourceProductSubtype()
        );
        if (id == null) {
            throw new IllegalStateException(
                    "loan_product_catalog 저장 후 catalog_product_id 조회에 실패했습니다: "
                            + catalog.sourceProductKey()
            );
        }
        return id;
    }

    public record SaveCount(int productCount, int optionCount) {
    }
}
