package com.via.shinvia.loan.catalog.controller;

import com.via.shinvia.loan.catalog.dto.response.LoanProductCatalogResponses;
import com.via.shinvia.loan.catalog.service.LoanProductCatalogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-product-catalogs")
@RequiredArgsConstructor
public class LoanProductCatalogController {

    private final LoanProductCatalogQueryService service;

    @GetMapping
    public LoanProductCatalogResponses.ListEnvelope list(
            @RequestParam(name = "loan_type", required = false) String loanType,
            @RequestParam(name = "active", defaultValue = "true") Boolean active
    ) {
        return LoanProductCatalogResponses.ListEnvelope.success(service.list(loanType, active));
    }

    @GetMapping("/{catalogProductId}")
    public LoanProductCatalogResponses.DetailEnvelope detail(
            @PathVariable Long catalogProductId
    ) {
        return LoanProductCatalogResponses.DetailEnvelope.success(
                service.detail(catalogProductId)
        );
    }
}
