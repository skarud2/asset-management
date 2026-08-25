package com.via.shinvia.loananalysis.service;

import com.via.shinvia.loananalysis.calculator.DebtPriorityCalculator;
import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.mapper.LoanAccountAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 부채 상환우선순위 서비스
@Service
@RequiredArgsConstructor
public class DebtPriorityService {

    private final LoanAccountAnalysisMapper
            loanAccountAnalysisMapper;

    private final DebtPriorityCalculator
            debtPriorityCalculator;


    // 사용자 부채 상환순위 계산
    public List<DebtPriorityResponseDTO> calculate(
            Long userId
    ) {
        // 사용자 대출 조회
        List<LoanAccountAnalysisDTO> loans =
                loanAccountAnalysisMapper
                        .findActiveLoansByUserId(
                                userId
                        );

        // 대출 없음 처리
        if (loans == null || loans.isEmpty()) {
            return List.of();
        }

        // 전체 대출잔액 계산
        BigDecimal totalLoanBalance =
                loans.stream()
                        .map(
                                LoanAccountAnalysisDTO
                                        ::getCurrentBalance
                        )
                        .filter(
                                balance -> balance != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        // 대출별 RPS 계산
        List<DebtPriorityResponseDTO> results =
                loans.stream()
                        .map(loan ->
                                debtPriorityCalculator
                                        .calculate(
                                                loan,
                                                totalLoanBalance
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        DebtPriorityResponseDTO
                                                ::getPriorityScore
                                ).reversed()
                        )
                        .toList();

        // 순위 결과 생성
        List<DebtPriorityResponseDTO> rankedResults =
                new ArrayList<>();

        for (int index = 0;
             index < results.size();
             index++) {

            DebtPriorityResponseDTO result =
                    results.get(index);

            // 순위 포함 결과 생성
            rankedResults.add(
                    DebtPriorityResponseDTO.builder()
                            .loanAccountId(
                                    result.getLoanAccountId()
                            )
                            .loanType(
                                    result.getLoanType()
                            )
                            .currentBalance(
                                    result.getCurrentBalance()
                            )
                            .interestRate(
                                    result.getInterestRate()
                            )
                            .rateType(
                                    result.getRateType()
                            )
                            .loanStatus(
                                    result.getLoanStatus()
                            )
                            .overdueScore(
                                    result.getOverdueScore()
                            )
                            .interestScore(
                                    result.getInterestScore()
                            )
                            .feeScore(
                                    result.getFeeScore()
                            )
                            .smallLoanScore(
                                    result.getSmallLoanScore()
                            )
                            .studentLoanScore(
                                    result.getStudentLoanScore()
                            )
                            .priorityScore(
                                    result.getPriorityScore()
                            )
                            .priorityRank(
                                    index + 1
                            )
                            .reason(
                                    result.getReason()
                            )
                            .build()
            );
        }

        return rankedResults;
    }
}