package com.via.shinvia.surplusfund.product.fund.service;

import com.via.shinvia.surplusfund.product.fund.dto.FundProductResponse;
import com.via.shinvia.surplusfund.product.fund.mapper.FundProductMapper;
import com.via.shinvia.surplusfund.product.fund.model.FundProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundProductService {
    private final FundProductMapper fundProductMapper;

    @Transactional(readOnly = true)
    public List<FundProductResponse> findAll() {

        return fundProductMapper.findActiveFundProducts()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FundProductResponse toResponse(FundProduct product) {

        return new FundProductResponse(
                product.getInvestmentProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getProviderName(),
                product.getCategory(),
                product.getDisclosureBaseDate(),
                product.getReturn1Month(),
                product.getReturn3Months(),
                product.getReturn6Months(),
                product.getReturn12Months(),
                product.getFundGrade(),
                product.getUpfrontFeeRate(),
                product.getTotalExpenseRate(),
                product.getSourceType()
        );
    }
}
