package com.via.shinvia.mydata.service;

import com.via.shinvia.account.dto.request.AccountSyncRequest;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.account.service.AccountSyncService;
import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.service.mydata.CardSyncService;
import com.via.shinvia.service.mydata.LoanAccountSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyDataSyncService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccountSyncService accountSyncService;
    private final MyDataConnectionService myDataConnectionService;
    private final LoanAccountSyncService loanAccountSyncService;
    private final CardSyncService cardSyncService;
    private final FinancialInstitutionService financialInstitutionService;

    public void syncAll(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        if (connectionId == null) {
            throw new IllegalStateException(
                    "연결된 마이데이터 정보가 없습니다."
            );
        }

        LocalDate today = LocalDate.now();
        List<String> orgCodes = financialInstitutionService.getBankOrgCodes();

        for (String orgCode : orgCodes) {

            AccountSyncRequest request =
                    new AccountSyncRequest(
                            orgCode,
                            today.minusMonths(3).format(FORMATTER),
                            today.format(FORMATTER),
                            100
                    );

            accountSyncService.sync(userId, request);
        }

        loanAccountSyncService.syncLoans(userId);
        cardSyncService.syncCards(userId, connectionId);
    }

}
