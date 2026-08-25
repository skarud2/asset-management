package com.via.shinvia.mydata.controller;

import com.via.shinvia.mydata.domain.MyDataConnection;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataConnectionController {

    private final MyDataAuthService myDataAuthService;
    private final MyDataConnectionService myDataConnectionService;
    private final CurrentUser currentUser;

    @GetMapping("/connection")
    public String connectionPage(Authentication authentication) {
        Long userId=currentUser.getUserId(authentication);
        if (myDataConnectionService.isConnected(userId)) {
            return "redirect:/mydata/result";
        }
        return "mydata/connection";
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("state") String state,
                           @RequestParam("code") String code) {
        Long connectionId = Long.valueOf(state);

        try{
            myDataAuthService.issueTokens(state, code);
            myDataConnectionService.completeConnection(connectionId);

            return "redirect:/mydata/result";
        }catch(Exception e) {
            myDataConnectionService.failConnection(connectionId);
            return "redirect:/mydata/connection?error";
        }
    }
}