package com.via.shinvia.stresstest.service;

import com.via.shinvia.stresstest.mapper.StressTestFinancialProfileMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// loan/ratesimulation의 ThresholdType.INCOME_RATIO가 같은 이유(월소득 데이터 없음)로 미구현이었던 부분.
// user_financial_profile.annual_income이 생기면서 이 기능에서는 조회 가능해졌다. 프로필이 없는 사용자는
// available=false.
@Service
public class AnnualIncomeProvider {

    private final StressTestFinancialProfileMapper financialProfileMapper;

    public AnnualIncomeProvider(StressTestFinancialProfileMapper financialProfileMapper) {
        this.financialProfileMapper = financialProfileMapper;
    }

    public Result findAnnualIncome(Long userId) {
        BigDecimal annualIncome = financialProfileMapper.findAnnualIncomeByUserId(userId);

        if (annualIncome == null) {
            return new Result(false, null);
        }

        return new Result(true, annualIncome);
    }

    public record Result(
            boolean available,
            BigDecimal annualIncome
    ) {
    }
}
