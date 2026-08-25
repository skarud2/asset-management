package com.via.shinvia.surplusfund.plan.model;

import com.via.shinvia.surplusfund.preference.model.*;

import java.math.BigDecimal;

public class SurplusFundPlan {

    private Long surplusFundPlanId;
    private final Long userId;
    private final BigDecimal operationAmount;
    private final InvestmentPurpose investmentPurpose;
    private final Integer investmentPeriodMonths;
    private final LossToleranceLevel lossToleranceLevel;
    private final LiquidityNeed liquidityNeed;
    private final ExperienceLevel experienceLevel;
    private final boolean surplusAmountConfirmed;
    private final boolean guideNoticeConfirmed;
    private final InvestmentStyle investmentStyle;
    private final String ruleVersion;

    public SurplusFundPlan(
            Long userId,
            BigDecimal operationAmount,
            InvestmentPurpose investmentPurpose,
            Integer investmentPeriodMonths,
            LossToleranceLevel lossToleranceLevel,
            LiquidityNeed liquidityNeed,
            ExperienceLevel experienceLevel,
            boolean surplusAmountConfirmed,
            boolean guideNoticeConfirmed,
            InvestmentStyle investmentStyle,
            String ruleVersion
    ) {
        this.userId = userId;
        this.operationAmount = operationAmount;
        this.investmentPurpose = investmentPurpose;
        this.investmentPeriodMonths = investmentPeriodMonths;
        this.lossToleranceLevel = lossToleranceLevel;
        this.liquidityNeed = liquidityNeed;
        this.experienceLevel = experienceLevel;
        this.surplusAmountConfirmed = surplusAmountConfirmed;
        this.guideNoticeConfirmed = guideNoticeConfirmed;
        this.investmentStyle = investmentStyle;
        this.ruleVersion = ruleVersion;
    }

    public Long getSurplusFundPlanId() {
        return surplusFundPlanId;
    }

    public void setSurplusFundPlanId(Long surplusFundPlanId) {
        this.surplusFundPlanId = surplusFundPlanId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getOperationAmount() {
        return operationAmount;
    }

    public InvestmentPurpose getInvestmentPurpose() {
        return investmentPurpose;
    }

    public Integer getInvestmentPeriodMonths() {
        return investmentPeriodMonths;
    }

    public LossToleranceLevel getLossToleranceLevel() {
        return lossToleranceLevel;
    }

    public LiquidityNeed getLiquidityNeed() {
        return liquidityNeed;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public boolean isSurplusAmountConfirmed() {
        return surplusAmountConfirmed;
    }

    public boolean isGuideNoticeConfirmed() {
        return guideNoticeConfirmed;
    }

    public InvestmentStyle getInvestmentStyle() {
        return investmentStyle;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }
}
