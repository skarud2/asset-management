package com.via.shinvia.surplusfund.product.fund.controller;

import com.via.shinvia.surplusfund.product.fund.dto.FundImportResponse;
import com.via.shinvia.surplusfund.product.fund.service.FundProductImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/investment-products/funds")
public class FundProductImportController {
    private final FundProductImportService fundProductImportService;

    @PostMapping("/import")
    public ResponseEntity<FundImportResponse> importFundCsv() {

        FundImportResponse response = fundProductImportService.importDefaultCsv();

        return ResponseEntity.ok(response);
    }
}
