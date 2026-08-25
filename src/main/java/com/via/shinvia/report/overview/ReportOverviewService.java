package com.via.shinvia.report.overview;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.service.CardQueryService;
import com.via.shinvia.finprofile.FinancialProfile;
import com.via.shinvia.finprofile.FinancialProfileService;
import com.via.shinvia.loan.account.entity.LoanAccount;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.report.overview.dto.ReportOverviewData;
import com.via.shinvia.service.mydata.LoanAccountSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportOverviewService {
    private final FinancialProfileService financialProfileService;
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;
    private final CardQueryService cardQueryService;
    private final LoanAccountSyncService loanAccountSyncService;

    public ReportOverviewData getOverview(Long userId) {

        ReportOverviewData.FinancialProfileSummary financialProfile = getFinancialProfileSummary(userId);
        ReportOverviewData.MyDataSummary myData = getMyDataSummary(userId);

        return new ReportOverviewData(financialProfile, myData);
    }

    private ReportOverviewData.FinancialProfileSummary getFinancialProfileSummary(Long userId) {
        FinancialProfile profile = financialProfileService.findFinancialProfileByUserId(userId);

        if (profile == null) {
            return new ReportOverviewData.FinancialProfileSummary(
                    false,
                    "-",
                    "-",
                    "-",
                    "-",
                    "-"
            );
        }
        return new ReportOverviewData.FinancialProfileSummary(
                true,
                formatWon(profile.getAnnualIncome()),
                profile.getIncomeType() == null
                        ? "-"
                        : profile.getIncomeType().getLabel(),
                profile.getEmploymentStatus() == null
                        ? "-"
                        : profile.getEmploymentStatus().getLabel(),
                formatCreditScore(profile.getCreditScore()),
                formatWon(profile.getLiquidAssetAmount())
        );
    }

    private ReportOverviewData.MyDataSummary getMyDataSummary(Long userId) {
        if (!myDataConnectionService.isConnected(userId)) {
            return new ReportOverviewData.MyDataSummary(
                    false,
                    0,
                    "-",
                    0,
                    0,
                    "-"
            );
        }
        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);
        List<CardAccount> cards = cardQueryService.getCardsByConnectionId(connectionId);

        List<LoanAccount> loans = loanAccountSyncService.getMyLoans(userId);

        BigDecimal totalAccountBalance = accounts.stream()
                        .map(Account::getCurrentBalance)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoanBalance = loans.stream()
                        .map(LoanAccount::getCurrentBalance)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportOverviewData.MyDataSummary(
                true,
                accounts.size(),
                formatWon(totalAccountBalance),
                cards.size(),
                loans.size(),
                formatWon(totalLoanBalance)
        );
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }

        return String.format(
                Locale.KOREA,
                "%,d원",
                amount.longValue()
        );
    }

    private String formatCreditScore(Integer creditScore) {
        if (creditScore == null) {
            return "-";
        }

        return String.format(
                Locale.KOREA,
                "%,d점",
                creditScore
        );
    }
}
