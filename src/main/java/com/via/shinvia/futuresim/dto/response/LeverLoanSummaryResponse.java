package com.via.shinvia.futuresim.dto.response;

import com.via.shinvia.futuresim.service.LeverIntensityCalculator;
import java.math.BigDecimal;

public record LeverLoanSummaryResponse(LeverIntensityCalculator.LeverType leverType, BigDecimal intensity,
                                       Integer diffMonths, LeverImpactResponse.LoanSummary loanSummary) {}
