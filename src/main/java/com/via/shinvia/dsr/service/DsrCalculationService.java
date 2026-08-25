package com.via.shinvia.dsr.service;

import com.via.shinvia.dsr.component.AnnualDebtPaymentCalculator;
import com.via.shinvia.dsr.dto.request.DsrCalculationRequestDto;
import com.via.shinvia.dsr.dto.result.ExcludedLoanDto;
import com.via.shinvia.dsr.dto.type.InterestRateType;
import com.via.shinvia.dsr.dto.type.LoanType;
import com.via.shinvia.dsr.dto.result.DsrCalculationResultDto;
import com.via.shinvia.dsr.dto.type.PropertyRegion;
import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileMapper;
import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.mapper.LoanAccountAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DsrCalculationService {
    private final FinancialProfileMapper financialProfileMapper;
    private final LoanAccountAnalysisMapper loanAccountAnalysisMapper;
    private final AnnualDebtPaymentCalculator annualDebtPaymentCalculator;

    private static final BigDecimal CAPITAL_MORTGAGE_STRESS_RATE = new BigDecimal("3.0");

    private static final BigDecimal DEFAULT_STRESS_RATE = new BigDecimal("1.5");

    private static final BigDecimal CREDIT_LOAN_THRESHOLD = new BigDecimal("100000000");

    public DsrCalculationResultDto calculate(Long userId, DsrCalculationRequestDto request){
        if (request == null) {
            throw new IllegalArgumentException(
                    "DSR 계산 요청 정보가 필요합니다."
            );
        }

        FinancialProfile profile=null;

        List<LoanAccountAnalysisDTO> existingLoans=List.of();

        if (userId != null) {
            profile = financialProfileMapper.findFinancialProfileByUserId(userId);

            List<LoanAccountAnalysisDTO> foundLoans = loanAccountAnalysisMapper.findActiveLoansByUserId(userId);

            if (foundLoans != null) {
                existingLoans = foundLoans;
            }
        }


        BigDecimal annualIncome =  determineAnnualIncome(profile, request);;

        BigDecimal newLoanAnnualDebtPayment = calculateNewLoanAnnualDebtPayment(request);

        ExistingLoanCalculationResult existingLoanResult = calculateExistingLoans(existingLoans);

        BigDecimal existingAnnualDebtPayment = existingLoanResult.annualDebtPayment();

        BigDecimal totalAnnualDebtPayment = existingAnnualDebtPayment.add(newLoanAnnualDebtPayment);

        BigDecimal dsrRate = calculateDsrRate(totalAnnualDebtPayment, annualIncome);


        //stress dsr 계산
        BigDecimal stressAdditionalRate =
                determineStressAdditionalRate(
                        request,
                        existingLoans
                );

        BigDecimal stressInterestRate =
                request.getExpectedInterestRate()
                        .add(stressAdditionalRate);

        BigDecimal stressNewLoanAnnualDebtPayment =
                calculateNewLoanAnnualDebtPayment(
                        request,
                        stressInterestRate
                );

        BigDecimal stressTotalAnnualDebtPayment =
                existingAnnualDebtPayment.add(
                        stressNewLoanAnnualDebtPayment
                );

        BigDecimal stressDsrRate =
                calculateDsrRate(
                        stressTotalAnnualDebtPayment,
                        annualIncome
                );


        return DsrCalculationResultDto.builder()
                .annualIncome(annualIncome)
                .existingAnnualDebtPayment(existingAnnualDebtPayment)
                .newLoanAnnualDebtPayment(newLoanAnnualDebtPayment)
                .totalAnnualDebtPayment(totalAnnualDebtPayment)
                .dsrRate(dsrRate)
                .excludedLoans(existingLoanResult.excludedLoans())
                .partialCalculation(!existingLoanResult.excludedLoans().isEmpty())
                .stressNewLoanAnnualDebtPayment(stressNewLoanAnnualDebtPayment)
                .stressTotalAnnualDebtPayment(stressTotalAnnualDebtPayment)
                .stressDsrRate(stressDsrRate)
                .build();

    }

        //금융프로필에 연소득 있으면 우선으로 가져옴
        private BigDecimal determineAnnualIncome(FinancialProfile profile, DsrCalculationRequestDto request) {
            if (profile != null
                    && profile.getAnnualIncome() != null
                    && profile.getAnnualIncome()
                    .compareTo(BigDecimal.ZERO) > 0) {

                return profile.getAnnualIncome();
            }

            BigDecimal requestedAnnualIncome = request.getAnnualIncome();

            if (requestedAnnualIncome != null
                    && requestedAnnualIncome
                    .compareTo(BigDecimal.ZERO) > 0) {

                return requestedAnnualIncome;
            }

            throw new IllegalArgumentException(
                    "연소득을 입력해주세요."
            );
        }


    private BigDecimal calculateDsrRate(BigDecimal annualDebtPayment, BigDecimal annualIncome) {
        if (annualDebtPayment == null || annualDebtPayment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "연간 원리금 상환액은 0 이상이어야 합니다."
            );
        }

        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "연소득은 0보다 커야 합니다."
            );
        }

        return annualDebtPayment.multiply(BigDecimal.valueOf(100)).divide(annualIncome, 2, RoundingMode.HALF_UP);
    }

    //TODO: 제외 대출 처리
    private ExistingLoanCalculationResult calculateExistingLoans(List<LoanAccountAnalysisDTO> existingLoans) {
        if (existingLoans == null || existingLoans.isEmpty()) {
            return new ExistingLoanCalculationResult(BigDecimal.ZERO, List.of());
        }

        BigDecimal totalAnnualPayment = BigDecimal.ZERO;

        for (LoanAccountAnalysisDTO loan : existingLoans) {
            validateExistingLoan(loan);

            BigDecimal annualPayment;

            if (isJeonseLoan(loan.getLoanType())) { //전세대출의 연간 DSR 상환액 = 대출잔액×적용금리
                annualPayment = annualDebtPaymentCalculator.calculateInterestOnly(
                        loan.getCurrentBalance(), loan.getInterestRate());
            } else {
                int remainingMonths = calculateRemainingMonths(loan.getMaturityAt());

                RepaymentType repaymentType = convertRepaymentType(loan.getRepaymentType());

                annualPayment = annualDebtPaymentCalculator.calculate(
                        loan.getCurrentBalance(), loan.getInterestRate(), remainingMonths, repaymentType);
            }

            totalAnnualPayment = totalAnnualPayment.add(annualPayment);
        }

        return new ExistingLoanCalculationResult(totalAnnualPayment,  List.of());
    }

    //남은 대출기간 계산
    private int calculateRemainingMonths(LocalDate maturityAt) {
        if (maturityAt == null) {
            throw new IllegalArgumentException(
                    "대출 만기일 정보가 없습니다."
            );
        }

        LocalDate today = LocalDate.now();

        if (!maturityAt.isAfter(today)) {
            throw new IllegalArgumentException(
                    "이미 만기된 대출입니다."
            );
        }

        long fullMonths = ChronoUnit.MONTHS.between(today, maturityAt);

        LocalDate calculatedDate = today.plusMonths(fullMonths);

        // 자투리 날짜 1개월로 침
        if (calculatedDate.isBefore(maturityAt)) {
            fullMonths++;
        }

        return Math.toIntExact(fullMonths);
    }

    private RepaymentType convertRepaymentType(String repaymentType) {
        if (repaymentType == null || repaymentType.isBlank()) {
            throw new IllegalArgumentException(
                    "기존 대출의 상환방식이 없습니다."
            );
        }

        return switch (repaymentType.trim().toUpperCase()) {
            case "원리금균등상환", "원리금균등", "EQUAL_PRINCIPAL_INTEREST" ->
                    RepaymentType.EQUAL_PRINCIPAL_INTEREST;

            case "원금균등상환", "원금균등", "EQUAL_PRINCIPAL" ->
                    RepaymentType.EQUAL_PRINCIPAL;

            case "거치식", "GRACE_PERIOD" -> RepaymentType.GRACE_PERIOD;

            case "만기일시상환", "만기일시", "BULLET_PAYMENT" ->
                    RepaymentType.BULLET_PAYMENT;

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 상환방식입니다: " + repaymentType
            );
        };
    }

    private boolean isJeonseLoan(LoanType loanType) {
        if (loanType == null) {
            return false;
        }

        return loanType == LoanType.JEONSE_LOAN;
    }

    private void validateExistingLoan(LoanAccountAnalysisDTO loan) {
        if (loan == null) {
            throw new IllegalArgumentException(
                    "기존 대출 정보가 없습니다."
            );
        }

        if (loan.getCurrentBalance() == null || loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "기존 대출잔액이 유효하지 않습니다."
            );
        }

        if (loan.getInterestRate() == null || loan.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "기존 대출금리가 유효하지 않습니다."
            );
        }

        if (!isJeonseLoan(loan.getLoanType()) && (loan.getRepaymentType() == null || loan.getRepaymentType().isBlank())) {
            throw new IllegalArgumentException(
                    "기존 대출의 상환방식이 없습니다."
            );
        }
    }

    //신규 대출 계산
    private BigDecimal calculateNewLoanAnnualDebtPayment(
            DsrCalculationRequestDto request
    ) {
        return calculateNewLoanAnnualDebtPayment(
                request,
                request.getExpectedInterestRate()
        );
    }

    private BigDecimal calculateNewLoanAnnualDebtPayment(DsrCalculationRequestDto request, BigDecimal appliedInterestRate) {
        int loanTermMonths = request.getLoanTermYears() * 12;

        if (isJeonseLoan(request.getLoanType())) {
            return annualDebtPaymentCalculator
                    .calculateInterestOnly(
                            request.getRequestAmount(),
                            appliedInterestRate
                    );
        }

        RepaymentType repaymentType = request.getRepaymentType() == null ? RepaymentType.EQUAL_PRINCIPAL_INTEREST
                                                                        : request.getRepaymentType();

        return annualDebtPaymentCalculator.calculate(
                request.getRequestAmount(),
                appliedInterestRate,
                loanTermMonths,
                repaymentType
        );
    }

    private record ExistingLoanCalculationResult(
            BigDecimal annualDebtPayment,
            List<ExcludedLoanDto> excludedLoans
    ) {
    }

    private BigDecimal determineStressAdditionalRate(
            DsrCalculationRequestDto request,
            List<LoanAccountAnalysisDTO> existingLoans
    ) {
        if (request.getInterestRateType()
                == InterestRateType.FIXED) {
            return BigDecimal.ZERO;
        }

        return switch (request.getLoanType()) {
            case MORTGAGE_LOAN ->
                    determineMortgageStressRate(request);

            case CREDIT_LOAN ->
                    determineCreditLoanStressRate(
                            request,
                            existingLoans
                    );

            case JEONSE_LOAN ->
                    DEFAULT_STRESS_RATE;

            case STUDENT_LOAN -> null;
        };
    }

    //주담대 가산 금리
    private BigDecimal determineMortgageStressRate(
            DsrCalculationRequestDto request
    ) {
        PropertyRegion region =
                request.getPropertyRegion();

        if (region == null) {
            throw new IllegalArgumentException(
                    "주택담보대출은 주택 지역이 필요합니다."
            );
        }

        if (region == PropertyRegion.CAPITAL_AREA
                || region == PropertyRegion.REGULATED_AREA) {
            return CAPITAL_MORTGAGE_STRESS_RATE;
        }

        return DEFAULT_STRESS_RATE;
    }

    //신용대출 가산 금리
    private BigDecimal determineCreditLoanStressRate(
            DsrCalculationRequestDto request,
            List<LoanAccountAnalysisDTO> existingLoans
    ) {
        BigDecimal existingCreditLoanBalance =
                BigDecimal.ZERO;

        if (existingLoans != null) {
            existingCreditLoanBalance =
                    existingLoans.stream()
                            .filter(loan -> loan != null)
                            .filter(loan ->
                                    loan.getLoanType()
                                            == LoanType.CREDIT_LOAN
                            )
                            .map(
                                    LoanAccountAnalysisDTO
                                            ::getCurrentBalance
                            )
                            .filter(balance -> balance != null)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );
        }

        BigDecimal totalCreditLoanBalance =
                existingCreditLoanBalance.add(
                        request.getRequestAmount()
                );

        if (totalCreditLoanBalance.compareTo(
                CREDIT_LOAN_THRESHOLD
        ) > 0) {
            return DEFAULT_STRESS_RATE;
        }

        return BigDecimal.ZERO;
    }
}
