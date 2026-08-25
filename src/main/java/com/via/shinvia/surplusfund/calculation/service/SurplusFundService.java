package com.via.shinvia.surplusfund.calculation.service;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.client.card.mapper.CardMapper;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.surplusfund.calculation.dto.SurplusFundCalculationSaveRequest;
import com.via.shinvia.surplusfund.calculation.entity.SurplusFundCalculation;
import com.via.shinvia.surplusfund.calculation.mapper.SurplusFundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurplusFundService {
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;
    private final CardMapper cardMapper;
    private final SurplusFundMapper surplusFundMapper;

    public BigDecimal calculateTotalCurrentBalance(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);

        return accounts.stream()
                .map(Account::getCurrentBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateScheduledCardAmount(Long userId) {

        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        BigDecimal amount = cardMapper.sumChargeAmountByUserAndMonth(userId, currentMonth);

        return amount != null ? amount : BigDecimal.ZERO;
    }

    public BigDecimal calculateAvailableSurplusAmount(Long userId) {

        BigDecimal totalCurrentBalance = calculateTotalCurrentBalance(userId);

        BigDecimal scheduledCardAmount = calculateScheduledCardAmount(userId);

        return totalCurrentBalance.subtract(scheduledCardAmount).max(BigDecimal.ZERO);
    }

    public BigDecimal calculateRecentAccountExpense(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        LocalDate today = LocalDate.now();

        LocalDateTime from = today.withDayOfMonth(1).minusMonths(2).atStartOfDay();

        LocalDateTime to = today.plusDays(1).atStartOfDay();

        return surplusFundMapper.sumAccountLivingExpense(connectionId, from, to);
    }

    public BigDecimal calculateRecentCardExpense(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        LocalDate today = LocalDate.now();

        LocalDateTime from = today.minusMonths(3).atStartOfDay();

        LocalDateTime to = today.plusDays(1).atStartOfDay();

        return surplusFundMapper.sumCardLivingExpense(connectionId, from, to);
    }

    public BigDecimal calculateRecentLivingExpense(Long userId) {

        BigDecimal accountExpense = calculateRecentAccountExpense(userId);

        BigDecimal cardExpense = calculateRecentCardExpense(userId);

        return accountExpense.add(cardExpense);
    }

    public BigDecimal calculateDailyAverageLivingExpense(Long userId) {

        BigDecimal recentLivingExpense = calculateRecentLivingExpense(userId);

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.withDayOfMonth(1).minusMonths(2);

        long analysisDays = ChronoUnit.DAYS.between(fromDate, today.plusDays(1));

        if (analysisDays <= 0) {
            return BigDecimal.ZERO;
        }

        return recentLivingExpense.divide(BigDecimal.valueOf(analysisDays), 0, RoundingMode.HALF_UP);
    }

    public LocalDate estimateNextIncomeDate(Long userId) {  //다음 급여일 추정 메서드

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        LocalDateTime latestSalaryDate = surplusFundMapper.findLatestSalaryIncomeDate(connectionId);

        if (latestSalaryDate == null) {
            return null;
        }

        LocalDate today = LocalDate.now();

        LocalDate estimatedDate = latestSalaryDate.toLocalDate().plusMonths(1);

        while (!estimatedDate.isAfter(today)) {
            estimatedDate = estimatedDate.plusMonths(1);
        }

        return estimatedDate;
    }

    public BigDecimal calculateEstimatedLivingExpense(
            Long userId,
            LocalDate nextIncomeDate
    ) {

        if (nextIncomeDate == null) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();

        if (!nextIncomeDate.isAfter(today)) {
            return BigDecimal.ZERO;
        }

        long remainingDays = ChronoUnit.DAYS.between(today, nextIncomeDate);

        BigDecimal dailyAverage = calculateDailyAverageLivingExpense(userId);

        return dailyAverage.multiply(
                BigDecimal.valueOf(remainingDays)
        );
    }

    public BigDecimal calculateRecommendedEmergencyFund(Long userId) {
        return BigDecimal.valueOf(1_000_000);
    }

    public BigDecimal calculateWithdrawableAmount(Long userId) {
        return calculateTotalCurrentBalance(userId);
    }

    public BigDecimal calculateAvailableSurplusAmount(
            BigDecimal withdrawableAmount,
            BigDecimal livingExpense,
            BigDecimal scheduledExpense,
            BigDecimal emergencyFund
    ) {

        BigDecimal result = withdrawableAmount
                .subtract(livingExpense)
                .subtract(scheduledExpense)
                .subtract(emergencyFund);

        return result.max(BigDecimal.ZERO);
    }

    @Transactional
    public Long saveCalculation(
            Long userId,
            SurplusFundCalculationSaveRequest request
    ) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        if (connectionId == null) {
            throw new IllegalStateException(
                    "연결된 마이데이터가 없습니다."
            );
        }


        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);


        Set<Long> requestedAccountIds = new HashSet<>(request.getSelectedAccountIds());


        List<Account> selectedAccounts = accounts.stream()
                        .filter(account -> requestedAccountIds.contains(account.getAccountId()))
                        .toList();


        if (selectedAccounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "여유자금 계산에 사용할 계좌를 선택해주세요."
            );
        }


        BigDecimal selectedAccountBalance =
                selectedAccounts.stream()
                        .map(Account::getCurrentBalance)
                        .filter(Objects::nonNull)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        String selectedAccountIds =
                selectedAccounts.stream()
                        .map(Account::getAccountId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));


        if (selectedAccountBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "여유자금 계산에 사용할 계좌를 선택해주세요."
            );
        }

        BigDecimal totalCurrentBalance = calculateTotalCurrentBalance(userId);
        LocalDate estimatedNextIncomeDate = estimateNextIncomeDate(userId);
        BigDecimal estimatedLivingExpense = calculateEstimatedLivingExpense(userId, estimatedNextIncomeDate);
        BigDecimal estimatedScheduledExpense = calculateScheduledCardAmount(userId);
        BigDecimal recommendedEmergencyFund = calculateRecommendedEmergencyFund(userId);


        BigDecimal calculatedSurplusAmount = calculateAvailableSurplusAmount(
                        selectedAccountBalance,
                        request.getAdjustedLivingExpense(),
                        request.getAdjustedScheduledExpense(),
                        request.getAdjustedEmergencyFund()
                );


        BigDecimal finalSurplusAmount = request.getFinalSurplusAmount();


        if (finalSurplusAmount.compareTo(calculatedSurplusAmount) > 0) {
            throw new IllegalArgumentException(
                    "최종 운용금액은 계산된 여유자금을 초과할 수 없습니다."
            );
        }


        SurplusFundCalculation calculation =
                SurplusFundCalculation.builder()
                        .userId(userId)
                        .connectionId(connectionId)
                        .totalCurrentBalance(totalCurrentBalance)
                        .selectedAccountBalance(selectedAccountBalance)
                        .selectedAccountIds(selectedAccountIds)
                        .estimatedNextIncomeDate(estimatedNextIncomeDate)
                        .adjustedNextIncomeDate(request.getAdjustedNextIncomeDate())
                        .estimatedLivingExpense(estimatedLivingExpense)
                        .adjustedLivingExpense(request.getAdjustedLivingExpense())
                        .estimatedScheduledExpense(estimatedScheduledExpense)
                        .adjustedScheduledExpense(request.getAdjustedScheduledExpense())
                        .recommendedEmergencyFund(recommendedEmergencyFund)
                        .adjustedEmergencyFund(request.getAdjustedEmergencyFund())
                        .calculatedSurplusAmount(calculatedSurplusAmount)
                        .finalSurplusAmount(finalSurplusAmount)
                        .build();


        surplusFundMapper.insertCalculation(calculation);


        return calculation.getSurplusFundCalculationId();
    }

    @Transactional(readOnly = true)
    public SurplusFundCalculation getLatestCalculation(Long userId) {
        return surplusFundMapper.findLatestCalculationByUserId(userId);
    }


    public List<Long> parseSelectedAccountIds(String selectedAccountIds) {

        if (selectedAccountIds == null || selectedAccountIds.isBlank()) {
            return List.of();
        }

        return Arrays.stream(selectedAccountIds.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(Long::valueOf)
                        .toList();
    }
}
