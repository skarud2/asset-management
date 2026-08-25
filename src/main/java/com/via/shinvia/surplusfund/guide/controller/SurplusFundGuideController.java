package com.via.shinvia.surplusfund.guide.controller;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.surplusfund.calculation.entity.SurplusFundCalculation;
import com.via.shinvia.surplusfund.calculation.service.SurplusFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class SurplusFundGuideController {

    private final CurrentUser currentUser;
    private final SurplusFundService surplusFundService;
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;

    @GetMapping("/surplus-funds/guide")
    public String guide(Authentication authentication, Model model) {

        Long userId = currentUser.getUserId(authentication);
        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        List<Account> accountList = accountQueryService.getAccountsByConnectionId(connectionId);

        LocalDateTime lastUpdatedAt = accountList.stream()
                .map(Account::getDataAsOfAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        BigDecimal totalCurrentBalance = surplusFundService.calculateTotalCurrentBalance(userId);

        //저장값이 없는 최초 진입 시 입출금 계좌(1001)를 기본 선택
        List<Long> defaultSelectedAccountIds =
                accountList.stream()
                        .filter(account ->
                                "1001".equals(
                                        account.getAccountType()
                                )
                        )
                        .map(Account::getAccountId)
                        .toList();


        BigDecimal defaultAvailableBalance =
                accountList.stream()
                        .filter(account ->
                                defaultSelectedAccountIds.contains(
                                        account.getAccountId()
                                )
                        )
                        .map(Account::getCurrentBalance)
                        .filter(Objects::nonNull)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        //화면에서 실제 사용할 선택 계좌 목록 최초에는 기본 선택 계좌
        List<Long> selectedAccountIds = defaultSelectedAccountIds;

        LocalDate estimatedNextIncomeDate = surplusFundService.estimateNextIncomeDate(userId);
        BigDecimal estimatedLivingExpense = surplusFundService.calculateEstimatedLivingExpense(userId, estimatedNextIncomeDate);
        BigDecimal estimatedScheduledExpense = surplusFundService.calculateScheduledCardAmount(userId);
        BigDecimal recommendedEmergencyFund = surplusFundService.calculateRecommendedEmergencyFund(userId);

        LocalDate nextIncomeDate = estimatedNextIncomeDate;
        BigDecimal livingExpense = estimatedLivingExpense;
        BigDecimal scheduledExpense = estimatedScheduledExpense;
        BigDecimal emergencyFund = recommendedEmergencyFund;

        BigDecimal availableSurplusAmount = surplusFundService.calculateAvailableSurplusAmount(
                defaultAvailableBalance,
                livingExpense,
                scheduledExpense,
                emergencyFund
        );

        BigDecimal operationAmount = availableSurplusAmount;

        boolean hasSavedCalculation = false;

        SurplusFundCalculation savedCalculation = surplusFundService.getLatestCalculation(userId);

        if (savedCalculation != null) {
            hasSavedCalculation = true;

            selectedAccountIds = surplusFundService.parseSelectedAccountIds(
                    savedCalculation.getSelectedAccountIds()
            );

            defaultAvailableBalance = savedCalculation.getSelectedAccountBalance();

            estimatedNextIncomeDate = savedCalculation.getEstimatedNextIncomeDate();
            estimatedLivingExpense = savedCalculation.getEstimatedLivingExpense();
            estimatedScheduledExpense = savedCalculation.getEstimatedScheduledExpense();
            recommendedEmergencyFund = savedCalculation.getRecommendedEmergencyFund();

            nextIncomeDate = savedCalculation.getAdjustedNextIncomeDate();
            livingExpense = savedCalculation.getAdjustedLivingExpense();
            scheduledExpense = savedCalculation.getAdjustedScheduledExpense();
            emergencyFund = savedCalculation.getAdjustedEmergencyFund();

            availableSurplusAmount = savedCalculation.getCalculatedSurplusAmount();
            operationAmount = savedCalculation.getFinalSurplusAmount();
        }

        model.addAttribute("accountList", accountList);
        model.addAttribute("lastUpdatedAt", lastUpdatedAt);
        model.addAttribute("totalCurrentBalance", totalCurrentBalance);
        model.addAttribute("selectedAccountIds", selectedAccountIds);
        model.addAttribute("defaultAvailableBalance", defaultAvailableBalance);

        model.addAttribute("estimatedNextIncomeDate", estimatedNextIncomeDate);
        model.addAttribute("estimatedLivingExpense", estimatedLivingExpense);
        model.addAttribute("estimatedScheduledExpense", estimatedScheduledExpense);
        model.addAttribute("recommendedEmergencyFund", recommendedEmergencyFund);

        model.addAttribute("nextIncomeDate", nextIncomeDate);
        model.addAttribute("livingExpense", livingExpense);
        model.addAttribute("scheduledExpense", scheduledExpense);
        model.addAttribute("emergencyFund", emergencyFund);

        model.addAttribute("availableSurplusAmount", availableSurplusAmount);
        model.addAttribute("operationAmount", operationAmount);
        model.addAttribute("hasSavedCalculation", hasSavedCalculation);

        return "surplusfund/guide";
    }
}