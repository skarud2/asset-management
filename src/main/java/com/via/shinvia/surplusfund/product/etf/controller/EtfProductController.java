package com.via.shinvia.surplusfund.product.etf.controller;

import com.via.shinvia.surplusfund.product.etf.dto.EtfProductListResponse;
import com.via.shinvia.surplusfund.product.etf.dto.EtfProductResponse;
import com.via.shinvia.surplusfund.product.etf.model.EtfProductSort;
import com.via.shinvia.surplusfund.product.etf.service.EtfProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surplus-funds/products/etfs")
public class EtfProductController {

    private final EtfProductService etfProductService;

    public EtfProductController(EtfProductService etfProductService) {
        this.etfProductService = etfProductService;
    }

    @GetMapping
    public ResponseEntity<EtfProductListResponse> findProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "TRADING_VALUE_DESC") EtfProductSort sort,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(
                etfProductService.findProducts(keyword, sort, limit)
        );
    }

    @GetMapping("/{investmentProductId}")
    public ResponseEntity<EtfProductResponse> findById(
            @PathVariable Long investmentProductId
    ) {
        return ResponseEntity.ok(
                etfProductService.findById(investmentProductId)
        );
    }
}

