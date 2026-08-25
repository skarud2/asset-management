package com.via.shinvia.surplusfund.preference.service;

import com.via.shinvia.surplusfund.preference.dto.InvestmentPreferenceRequest;
import com.via.shinvia.surplusfund.preference.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 필수 확인사항검사 -> 문항별 점수 계산 -> 안정형 보호조건 심사 -> 최종 성향 판정 -> 성향/점수/근거 반환
@Component
public class InvestmentStyleClassifier {

    // 우리서비스가 만든 교육용 가이드 규칙
    public static final String RULE_VERSION = "GUIDE_V1.0";

    // 계산 지휘
    public ClassificationResult classify (InvestmentPreferenceRequest request) {
        validateConfirmations(request);// 필수 확인사항 검사하는 부분임

        List<String> reasons = new ArrayList<>();

        int score = 0;
        score += purposeScore(request, reasons);
        score += periodScore(request.investmentPeriodMonths(), reasons);
        score += liquidityScore(request, reasons);
        score += lossToleranceScore(request, reasons);
        score += experienceScore(request, reasons);


        // 결과를 묶어서 반환
        InvestmentStyle style = determineStyle(request, score, reasons);
        return new ClassificationResult(style, score, RULE_VERSION, reasons);



    }


    private void validateConfirmations(InvestmentPreferenceRequest request) {
        if (!Boolean.TRUE.equals (request.surplusAmountConfirmed())) {
            throw new IllegalArgumentException("생활비와 대출 상환액을 제외한 여유자금인지 확인해야 합니다.");
        }

        if (!Boolean.TRUE.equals(request.guideNoticeConfirmed())) {
            throw new IllegalArgumentException("교육용 운용 가이드 안내를 확인해야 합니다.");
        }
    }


    private int purposeScore (InvestmentPreferenceRequest request, List<String> reasons) {
        int score = switch (request.investmentPurpose()) {
            case PRINCIPAL_PROTECTION -> 0;
            case STABLE_RETURN -> 1;
            case BALANCED_GROWTH -> 2;
            case CAPITAL_GROWTH -> 3;
        };
        reasons.add("운용 목적 "+ request.investmentPurpose() + " : " + score + "점");
        return score;
    }

    private int periodScore (int months, List<String> reasons) {
        int score;
        if (months <=12) {
            score = 0;
        }else if (months <=36) {
            score = 1;
        }else {
            score = 2;
        }

        reasons.add("투자기간 " + months + "개월: " + score + "점");
        return score;
    }

    private int lossToleranceScore (InvestmentPreferenceRequest request, List<String> reasons) {
        int score = switch (request.lossToleranceLevel()) {
            case NO_LOSS ->0;
            case WITHIN_5_PERCENT -> 1;
            case WITHIN_15_PERCENT -> 3;
            case OVER_15_PERCENT -> 4;
        };
        reasons.add("손실감내 수준 "+ request.lossToleranceLevel() + ": "+ score + "점");
        return score;
    }

    private int liquidityScore(InvestmentPreferenceRequest request, List<String> reasons) {
        int score = switch (request.liquidityNeed()) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
        reasons.add("유동성 필요 "+ request.liquidityNeed() + ": " + score + "점");
        return score;
    }

    private int experienceScore(InvestmentPreferenceRequest request, List<String> reasons) {
        int score = switch (request.experienceLevel()) {
            case NONE ->0;
            case LIMITED ->1;
            case UNDERSTANDS_RISK ->2;
        };
        reasons.add("경험 및 이해 수준 " + request.experienceLevel() +": " + score + "점");
        return score;
    }

    private InvestmentStyle determineStyle( InvestmentPreferenceRequest request, int score, List<String> reasons) {
        if (request.investmentPurpose() == InvestmentPurpose.PRINCIPAL_PROTECTION) {
            reasons.add("운용목적이 원금보존이므로 안정형 보호 규칙을 적용합니다.");
            return InvestmentStyle.STABLE;
        }

        if (request.lossToleranceLevel() == LossToleranceLevel.NO_LOSS) {
            reasons.add("손실을 원하지 않으므로 안정형 보호 규칙을 적용합니다.");
            return InvestmentStyle.STABLE;
        }

        if (request.investmentPeriodMonths() <= 12 && request.liquidityNeed() == LiquidityNeed.HIGH) {
            reasons.add("1년 이내 사용 가능성이 높으므로 안정형 보호 규칙을 적용합니다.");
            return InvestmentStyle.STABLE;
        }

        if (score <= 4) {
            reasons.add("총점 " + score + "점으로 안정형 기준에 해당합니다.");
            return InvestmentStyle.STABLE;
        }


        boolean aggressiveEligible = score >= 10 && request.investmentPurpose() == InvestmentPurpose.CAPITAL_GROWTH && request.investmentPeriodMonths() > 36 && request.lossToleranceLevel() == LossToleranceLevel.OVER_15_PERCENT && request.experienceLevel() == ExperienceLevel.UNDERSTANDS_RISK;

        if (aggressiveEligible) {
            reasons.add("총점 " + score + "점이며 공격형 필수 조건을 충족합니다.");
            return InvestmentStyle.AGGRESSIVE;
        }


        if (score >= 10) {
            reasons.add("총점은 공격형 구간이지만 필수 조건을 충족하지 않으므로 균형형을 적용합니다. ");

        } else {
            reasons.add("총점 " + score + "점으로 균형형 기준에 해당합니다.");

        }
        return InvestmentStyle.BALANCED;

    }

    public record  ClassificationResult (
            InvestmentStyle investmentStyle, int score, String ruleVersion, List<String> reasons
    ) {
        public ClassificationResult {
            reasons = List.copyOf(reasons);
        }
    }
}

