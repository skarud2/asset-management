package com.via.shinvia.loananalysis.service;

import com.via.shinvia.loananalysis.calculator.LoanScenarioCalculator;
import com.via.shinvia.loananalysis.dto.FinancialCapacityDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioRequestDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioResponseDTO;
import com.via.shinvia.loananalysis.mapper.FinancialCapacityMapper;
import com.via.shinvia.loananalysis.mapper.LoanAccountAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 대출 대안 비교 서비스
@Service
@RequiredArgsConstructor
public class LoanScenarioService {

    private final LoanAccountAnalysisMapper
            loanAccountAnalysisMapper;

    private final FinancialCapacityMapper
            financialCapacityMapper;

    private final LoanScenarioCalculator
            loanScenarioCalculator;


    // 네 가지 대안 비교
    public List<LoanScenarioResponseDTO> calculate(
            Long userId,
            LoanScenarioRequestDTO request
    ) {
        // 요청값 확인
        validateRequest(request);

        // 분석할 대출 조회
        LoanAccountAnalysisDTO loan =
                loanAccountAnalysisMapper
                        .findLoanById(
                                userId,
                                request.getTargetLoanAccountId()
                        );

        // 대출 없음 처리
        if (loan == null) {
            throw new IllegalArgumentException(
                    "분석할 대출정보가 없습니다."
            );
        }

        // 사용자 재무정보 조회
        FinancialCapacityDTO financial =
                financialCapacityMapper
                        .findFinancialCapacityByUserId(
                                userId
                        );

        // 재무정보 없음 처리
        if (financial == null) {
            throw new IllegalArgumentException(
                    "사용자 재무정보가 없습니다."
            );
        }

        // 네 가지 대안 반환
        return List.of(
                loanScenarioCalculator
                        .calculateKeep(
                                loan,
                                financial
                        ),

                loanScenarioCalculator
                        .calculatePartialRepayment(
                                loan,
                                financial,
                                request
                        ),

                loanScenarioCalculator
                        .calculateRefinance(
                                loan,
                                financial,
                                request
                        ),

                loanScenarioCalculator
                        .calculateCashHolding(
                                loan,
                                financial
                        )
        );
    }


    // 요청값 검증
    private void validateRequest(
            LoanScenarioRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "요청정보가 없습니다."
            );
        }

        if (request.getTargetLoanAccountId() == null) {
            throw new IllegalArgumentException(
                    "분석할 대출번호가 필요합니다."
            );
        }
    }

    // 로그인 사용자의 분석 가능 대출 조회
    public List<LoanAccountAnalysisDTO> findActiveLoans(Long userId) {
        return loanAccountAnalysisMapper.findActiveLoansByUserId(userId);
    }
}
