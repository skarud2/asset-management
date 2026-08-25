package com.via.shinvia.finprofile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialProfileService {
    private final FinancialProfileMapper fProfileMapper;

    @Transactional
    public void createFinancialProfile(FinancialProfileRequestDto request, Long userId) {
        FinancialProfile fProfile= FinancialProfile.builder()
                                                    .userId(userId)
                                                    .annualIncome(request.getAnnualIncome())
                                                    .incomeType(request.getIncomeType())
                                                    .employmentStatus(request.getEmploymentStatus())
                                                    .creditScore(request.getCreditScore())
                                                    .liquidAssetAmount(request.getLiquidAssetAmount())
                                                    .build();
        int insertCount = fProfileMapper.insertFinancialProfile(fProfile);
        if (insertCount!=1) {
            throw new IllegalArgumentException("저장 실패!");
        }
    }

    @Transactional
    public void updateFinancialProfile(FinancialProfileRequestDto request, Long userId) {
        FinancialProfile fProfile= FinancialProfile.builder()
                .userId(userId)
                .annualIncome(request.getAnnualIncome())
                .incomeType(request.getIncomeType())
                .employmentStatus(request.getEmploymentStatus())
                .creditScore(request.getCreditScore())
                .liquidAssetAmount(request.getLiquidAssetAmount())
                .build();
        int updateCount = fProfileMapper.updateFinancialProfile(fProfile);
        if (updateCount!=1) {
            throw new IllegalArgumentException("수정 실패!");
        }
    }


    public FinancialProfile findFinancialProfileByUserId(Long userId) {
        return fProfileMapper.findFinancialProfileByUserId(userId);
    }
}
