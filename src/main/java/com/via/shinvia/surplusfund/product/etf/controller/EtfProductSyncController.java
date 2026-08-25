package com.via.shinvia.surplusfund.product.etf.controller;

import com.via.shinvia.surplusfund.product.etf.dto.EtfSyncResponse;
import com.via.shinvia.surplusfund.product.etf.service.EtfProductSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/investment-products/etfs")
public class EtfProductSyncController {

    private final EtfProductSyncService etfProductSyncService;

    public EtfProductSyncController(EtfProductSyncService etfProductSyncService) {
        this.etfProductSyncService = etfProductSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<EtfSyncResponse> sync(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate
    ) {
        return ResponseEntity.ok(etfProductSyncService.sync(baseDate));
    }
}

