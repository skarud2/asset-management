package com.via.shinvia.surplusfund.product.fund.controller;

import com.via.shinvia.surplusfund.product.fund.dto.FundProductResponse;
import com.via.shinvia.surplusfund.product.fund.service.FundProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/surplus-funds/products/funds")
public class FundProductController {
    private final FundProductService fundProductService;

    @GetMapping
    public ResponseEntity<List<FundProductResponse>> getFunds() {

        return ResponseEntity.ok(
                fundProductService.findAll()
        );
    }
}
