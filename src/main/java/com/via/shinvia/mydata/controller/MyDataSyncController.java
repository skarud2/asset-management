package com.via.shinvia.mydata.controller;

import com.via.shinvia.mydata.service.MyDataSyncService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataSyncController {
    private final MyDataSyncService myDataSyncService;
    private final CurrentUser currentUser;

    @PostMapping("/sync")
    public String sync(
            Authentication authentication,  @RequestParam(required = false) String returnTo
    ) {
        Long userId = currentUser.getUserId(authentication);

        myDataSyncService.syncAll(userId);

        if ("/surplus-funds/guide".equals(returnTo)) {
            return "redirect:/surplus-funds/guide";
        }

        return "redirect:/mydata/result";
    }
}
