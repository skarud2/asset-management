package com.via.shinvia.loan.recommendation.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcludedLoanProduct {

    private Long catalogProductId;
    private String productName;
    private String institutionName;
    private String targetDescription;
    private List<String> reasons;
}
