package com.via.shinvia.surplusfund.preference.dto;

// 투자 기간 / 손실 감내 / 원금 보존 등 설문값


import com.via.shinvia.surplusfund.preference.model.ExperienceLevel;
import com.via.shinvia.surplusfund.preference.model.InvestmentPurpose;
import com.via.shinvia.surplusfund.preference.model.LiquidityNeed;
import com.via.shinvia.surplusfund.preference.model.LossToleranceLevel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InvestmentPreferenceRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal operationAmount,

        @NotNull
        InvestmentPurpose investmentPurpose,

        @NotNull
        @Min(1)
        @Max(600)
        Integer investmentPeriodMonths,

        @NotNull
        LossToleranceLevel lossToleranceLevel,

        @NotNull
        LiquidityNeed liquidityNeed,

        @NotNull
        ExperienceLevel experienceLevel,

        @NotNull
        @AssertTrue
        Boolean surplusAmountConfirmed,

        @NotNull
        @AssertTrue
        Boolean guideNoticeConfirmed
) {
}