package com.via.shinvia.loan.catalog.service;

import com.via.shinvia.loan.catalog.client.FinlifeLoanProductClient;
import com.via.shinvia.loan.catalog.converter.LoanProductCatalogConverter;
import com.via.shinvia.loan.catalog.dto.command.LoanProductCatalogModels;
import com.via.shinvia.loan.catalog.dto.external.credit.CreditResponse;
import com.via.shinvia.loan.catalog.dto.external.jeonse.JeonseResponse;
import com.via.shinvia.loan.catalog.dto.external.mortgage.MortgageResponse;
import com.via.shinvia.loan.catalog.dto.response.LoanProductCatalogResponses;
import com.via.shinvia.loan.catalog.exception.FinlifeApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductCatalogSyncService {

    private final FinlifeLoanProductClient client;
    private final LoanProductCatalogConverter converter;
    private final LoanProductCatalogPersistenceService persistenceService;

    public LoanProductCatalogResponses.SyncResult mortgage() {
        List<LoanProductCatalogModels.HousingProduct> allProducts = new ArrayList<>();
        int page = 1;
        int pages = 0;

        while (true) {
            MortgageResponse response = client.fetchMortgage(page);
            MortgageResponse.Result result = require(response == null ? null : response.result());
            validate(result.errorCode(), result.errorMessage());
            allProducts.addAll(converter.mortgage(result.baseList(), result.optionList()));
            pages++;

            int maxPage = result.maxPageNo() == null ? page : result.maxPageNo();
            if (page >= maxPage) break;
            page++;
        }

        var count = persistenceService.replaceHousingCatalog("MORTGAGE", allProducts);
        return new LoanProductCatalogResponses.SyncResult(
                "MORTGAGE", pages, count.productCount(), count.optionCount()
        );
    }

    public LoanProductCatalogResponses.SyncResult jeonse() {
        List<LoanProductCatalogModels.HousingProduct> allProducts = new ArrayList<>();
        int page = 1;
        int pages = 0;

        while (true) {
            JeonseResponse response = client.fetchJeonse(page);
            JeonseResponse.Result result = require(response == null ? null : response.result());
            validate(result.errorCode(), result.errorMessage());
            allProducts.addAll(converter.jeonse(result.baseList(), result.optionList()));
            pages++;

            int maxPage = result.maxPageNo() == null ? page : result.maxPageNo();
            if (page >= maxPage) break;
            page++;
        }

        var count = persistenceService.replaceHousingCatalog("JEONSE", allProducts);
        return new LoanProductCatalogResponses.SyncResult(
                "JEONSE", pages, count.productCount(), count.optionCount()
        );
    }

    public LoanProductCatalogResponses.SyncResult credit() {
        List<LoanProductCatalogModels.CreditProduct> allProducts = new ArrayList<>();
        int page = 1;
        int pages = 0;

        while (true) {
            CreditResponse response = client.fetchCredit(page);
            CreditResponse.Result result = require(response == null ? null : response.result());
            validate(result.errorCode(), result.errorMessage());
            allProducts.addAll(converter.credit(result.baseList(), result.optionList()));
            pages++;

            int maxPage = result.maxPageNo() == null ? page : result.maxPageNo();
            if (page >= maxPage) break;
            page++;
        }

        var count = persistenceService.replaceCreditCatalog(allProducts);
        return new LoanProductCatalogResponses.SyncResult(
                "CREDIT", pages, count.productCount(), count.optionCount()
        );
    }

    public List<LoanProductCatalogResponses.SyncResult> allFinlife() {
        return List.of(mortgage(), jeonse(), credit());
    }

    private <T> T require(T result) {
        if (result == null) {
            throw new IllegalStateException("Empty Finlife response");
        }
        return result;
    }

    private void validate(String code, String message) {
        if (!"000".equals(code)) {
            throw new FinlifeApiException(code, message);
        }
    }
}
