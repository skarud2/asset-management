package com.via.shinvia.loananalysis.calculator;

import com.via.shinvia.dsr.dto.type.LoanType;
import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.via.shinvia.dsr.dto.type.LoanType.STUDENT_LOAN;

// 부채 상환 우선순위 계산기
@Component
public class DebtPriorityCalculator {

    // 연체 가중치
    private static final BigDecimal OVERDUE_WEIGHT =
            new BigDecimal("0.40");

    // 금리 가중치
    private static final BigDecimal INTEREST_WEIGHT =
            new BigDecimal("0.30");

    // 수수료 가중치
    private static final BigDecimal FEE_WEIGHT =
            new BigDecimal("0.15");

    // 소액대출 가중치
    private static final BigDecimal SMALL_LOAN_WEIGHT =
            new BigDecimal("0.10");

    // 학자금대출 가중치
    private static final BigDecimal STUDENT_LOAN_WEIGHT =
            new BigDecimal("0.05");


    // 대출 1건 RPS 계산
    public DebtPriorityResponseDTO calculate(
            LoanAccountAnalysisDTO loan,
            BigDecimal totalLoanBalance
    ) {

        // 연체 점수 계산
        BigDecimal overdueScore =
                calculateOverdueScore(
                        loan.getLoanStatus()
                );

        // 금리 점수 계산
        BigDecimal interestScore =
                defaultZero(
                        loan.getInterestRate()
                );

        // 수수료 점수 계산
        BigDecimal feeScore =
                calculateFeeScore(
                        loan.getPrepaymentFeeRate(),
                        loan.getPrepaymentFeeEndDate()
                );

        // 소액대출 점수 계산
        BigDecimal smallLoanScore =
                calculateSmallLoanScore(
                        loan.getCurrentBalance(),
                        totalLoanBalance
                );

        // 학자금대출 점수 계산
        BigDecimal studentLoanScore =
                calculateStudentLoanScore(
                        loan.getLoanType()
                );


        // 연체 가중점수
        BigDecimal weightedOverdue =
                overdueScore.multiply(
                        OVERDUE_WEIGHT
                );

        // 금리 가중점수
        BigDecimal weightedInterest =
                interestScore.multiply(
                        INTEREST_WEIGHT
                );

        // 수수료 가중점수
        BigDecimal weightedFee =
                feeScore.multiply(
                        FEE_WEIGHT
                );

        // 소액대출 가중점수
        BigDecimal weightedSmallLoan =
                smallLoanScore.multiply(
                        SMALL_LOAN_WEIGHT
                );

        // 학자금대출 가중점수
        BigDecimal weightedStudentLoan =
                studentLoanScore.multiply(
                        STUDENT_LOAN_WEIGHT
                );


        // 최종 RPS 계산
        BigDecimal finalScore =
                weightedOverdue
                        .add(weightedInterest)
                        .subtract(weightedFee)
                        .add(weightedSmallLoan)
                        .subtract(weightedStudentLoan)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        // 추천 사유 생성
        String reason =
                createReason(
                        loan,
                        overdueScore,
                        feeScore,
                        smallLoanScore,
                        studentLoanScore
                );


        // 계산 결과 반환
        return DebtPriorityResponseDTO.builder()
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .loanType(
                        loan.getLoanType()
                )
                .currentBalance(
                        defaultZero(
                                loan.getCurrentBalance()
                        )
                )
                .interestRate(
                        interestScore
                )
                .rateType(
                        loan.getRateType()
                )
                .loanStatus(
                        loan.getLoanStatus()
                )
                .overdueScore(
                        overdueScore
                )
                .interestScore(
                        interestScore
                )
                .feeScore(
                        feeScore
                )
                .smallLoanScore(
                        smallLoanScore
                )
                .studentLoanScore(
                        studentLoanScore
                )
                .priorityScore(
                        finalScore
                )
                .reason(
                        reason
                )
                .build();
    }


    // 연체 점수 계산
    private BigDecimal calculateOverdueScore(
            String loanStatus
    ) {

        // 연체이면 100점
        if ("연체".equals(loanStatus)) {
            return BigDecimal.valueOf(100);
        }

        // 정상이면 0점
        return BigDecimal.ZERO;
    }


    // 중도상환수수료 점수 계산
    private BigDecimal calculateFeeScore(
            BigDecimal feeRate,
            LocalDate feeEndDate
    ) {

        // 수수료율 없음
        if (feeRate == null
                || feeRate.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.ZERO;
        }

        // 종료일 없음
        if (feeEndDate == null) {
            return feeRate.multiply(
                    BigDecimal.TEN
            );
        }

        // 수수료 기간 종료
        if (feeEndDate.isBefore(
                LocalDate.now()
        )) {
            return BigDecimal.ZERO;
        }

        // 수수료율 × 10
        return feeRate.multiply(
                BigDecimal.TEN
        );
    }


    // 소액대출 점수 계산
    private BigDecimal calculateSmallLoanScore(
            BigDecimal currentBalance,
            BigDecimal totalLoanBalance
    ) {

        // 대출잔액 null 처리
        BigDecimal balance =
                defaultZero(currentBalance);

        // 총대출잔액 null 처리
        BigDecimal totalBalance =
                defaultZero(totalLoanBalance);

        // 총잔액이 0이면 계산 불가
        if (totalBalance.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.ZERO;
        }

        // 해당 대출 잔액 비율
        BigDecimal balanceRatio =
                balance.divide(
                        totalBalance,
                        10,
                        RoundingMode.HALF_UP
                );

        // 100 × (1 - 잔액 비율)
        BigDecimal score =
                BigDecimal.ONE
                        .subtract(balanceRatio)
                        .multiply(
                                BigDecimal.valueOf(100)
                        );

        // 음수 방지 및 소수점 정리
        return score
                .max(BigDecimal.ZERO)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // 학자금대출 점수 계산
    private BigDecimal calculateStudentLoanScore(
            LoanType loanType
    ) {

        // 대출 종류 없음
        if (loanType == null) {
            return BigDecimal.ZERO;
        }

        // 학자금대출이면 100점
        if (loanType==STUDENT_LOAN) {
            return BigDecimal.valueOf(100);
        }

        // 일반대출이면 0점
        return BigDecimal.ZERO;
    }


    // 추천 사유 생성
    private String createReason(
            LoanAccountAnalysisDTO loan,
            BigDecimal overdueScore,
            BigDecimal feeScore,
            BigDecimal smallLoanScore,
            BigDecimal studentLoanScore
    ) {

        StringBuilder reason =
                new StringBuilder();

        // 연체 사유
        if (overdueScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {
            reason.append(
                    "현재 연체 중이므로 최우선 상환이 필요합니다. "
            );
        }

        // 고금리 사유
        if (defaultZero(
                loan.getInterestRate()
        ).compareTo(
                BigDecimal.valueOf(7)
        ) >= 0) {

            reason.append(
                    "금리가 높은 대출입니다. "
            );

            // 중금리 사유
        } else if (defaultZero(
                loan.getInterestRate()
        ).compareTo(
                BigDecimal.valueOf(5)
        ) >= 0) {

            reason.append(
                    "중금리 대출입니다. "
            );
        }

        // 수수료 사유
        if (feeScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            reason.append(
                    "중도상환수수료가 있어 즉시 상환의 실익을 확인해야 합니다. "
            );
        }

        // 소액대출 사유
        if (smallLoanScore.compareTo(
                BigDecimal.valueOf(80)
        ) >= 0) {

            reason.append(
                    "전체 부채에서 차지하는 비중이 작아 빠른 정리가 가능합니다. "
            );
        }

        // 학자금대출 사유
        if (studentLoanScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            reason.append(
                    "학자금대출은 상대적으로 상환 우선순위를 낮게 적용했습니다. "
            );
        }

        // 기본 사유
        if (reason.isEmpty()) {
            reason.append(
                    "금리와 잔액 비중을 기준으로 순위를 산정했습니다."
            );
        }

        return reason.toString().trim();
    }


    // null 금액 처리
    private BigDecimal defaultZero(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}