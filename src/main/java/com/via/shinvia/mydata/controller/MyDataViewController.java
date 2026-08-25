package com.via.shinvia.mydata.controller;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.client.card.entity.CardAccount;
import com.via.shinvia.client.card.service.CardQueryService;
import com.via.shinvia.loan.account.entity.LoanAccount;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.service.mydata.LoanAccountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataViewController {

    private final CurrentUser currentUser;
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;
    private final LoanAccountSyncService loanAccountSyncService;
    private final CardQueryService cardQueryService;
    // 화면 표시(myDataCardService, 목서버 실시간 조회)와 별개로 card_account 저장까지 같이 해준다.
    // 기존엔 이 화면이 보여주기만 하고 저장을 안 해서, 연동 직후엔 DSR/스트레스테스트 등
    // card_account를 참조하는 다른 기능들이 데이터를 전혀 못 보는 문제가 있었다.

    @GetMapping("/result")
    public String resultPage(Authentication authentication, Model model) {
        Long userId = currentUser.getUserId(authentication);
        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        if (connectionId == null) {
            return "redirect:/mydata/connection";
        }
        List<Account> accountList = accountQueryService.getAccountsByConnectionId(connectionId);

        model.addAttribute("accountList", accountList);
        model.addAttribute("accountCnt", accountList.size());

        List<LoanAccount> loanList = loanAccountSyncService.getMyLoans(userId);

        model.addAttribute("loanList", loanList);
        model.addAttribute("loanCnt", loanList.size());

        List<CardAccount> cardList = cardQueryService.getCardsByConnectionId(connectionId);

        model.addAttribute("cards", cardList);
        model.addAttribute("cardCnt", cardList.size());

        return "mydata/result";
    }
}
