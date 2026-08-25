package com.via.shinvia.mydata.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyDataLoginService {

    private final MyDataConnectionService myDataConnectionService;
    private final MyDataAuthService myDataAuthService;

    public void refreshTokenOnLogin(Long userId) {
        if(!myDataConnectionService.isConnected(userId)) {
            return ;
        }

        Long connectionId= myDataConnectionService.getConnectedConnectionId(userId);
        if (connectionId == null) {
            return;
        }

        myDataAuthService.refreshAccessToken(connectionId);
    }
}
