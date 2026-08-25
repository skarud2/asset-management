package com.via.shinvia.loan.catalog.controller;

import com.via.shinvia.loan.catalog.dto.response.LoanProductCatalogResponses;
import com.via.shinvia.loan.catalog.service.LoanProductCatalogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loan-product-catalogs/sync")
@RequiredArgsConstructor
public class LoanProductCatalogSyncController {

    private final LoanProductCatalogSyncService service;

    @PostMapping("/mortgage")
    public LoanProductCatalogResponses.SyncResult mortgage() {
        return service.mortgage();
    }

    @PostMapping("/jeonse")
    public LoanProductCatalogResponses.SyncResult jeonse() {
        return service.jeonse();
    }

    @PostMapping("/credit")
    public LoanProductCatalogResponses.SyncResult credit() {
        return service.credit();
    }

    @PostMapping("/finlife")
    public List<LoanProductCatalogResponses.SyncResult> allFinlife() {
        return service.allFinlife();
    }
}
