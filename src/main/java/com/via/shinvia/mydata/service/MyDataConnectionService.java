package com.via.shinvia.mydata.service;

import com.via.shinvia.mydata.domain.ConnectionStatus;
import com.via.shinvia.mydata.domain.MyDataConnection;
import com.via.shinvia.mydata.mapper.MyDataConnectionMapper;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyDataConnectionService {
    private final MyDataConnectionMapper myDataConnectionMapper;
    private final CurrentUser currentUser;

    public boolean isConnected(Long userId) {
        MyDataConnection connection = myDataConnectionMapper.findByUserId(userId);

        return connection != null
                && connection.getConnectionStatus() == ConnectionStatus.CONNECTED;
    }

    public Long startConnection(Long userId){
        MyDataConnection existingConnection = myDataConnectionMapper.findByUserId(userId);

        //최초 연동
        if(existingConnection==null) {
            MyDataConnection newConnection = MyDataConnection.builder()
                    .userId(userId)
                    .connectionStatus(ConnectionStatus.PENDING)
                    .build();
            int result = myDataConnectionMapper.insertMyDataConnection(newConnection);

            if(result!=1 || newConnection.getConnectionId()==null) {
                throw new IllegalStateException("마이데이터 연동 실패");
            }

            return newConnection.getConnectionId();
        }

        Long connectionId = existingConnection.getConnectionId();
        ConnectionStatus status = existingConnection.getConnectionStatus();

        // 이미 연결됐거나 현재 연동 중이면 기존 connectionId 사용
        if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.PENDING) {
            return connectionId;
        }

        // FAILED 등 다시 연동할 수 있는 상태
        int result = myDataConnectionMapper.updateStatus(connectionId, ConnectionStatus.PENDING);

        if (result != 1) {
            throw new IllegalStateException("마이데이터 연동 상태 변경에 실패했습니다.");
        }
        return connectionId;
    }

    public void completeConnection(Long connectionId) {
        int result = myDataConnectionMapper.updateConnected(connectionId);
        if (result!=1){
            throw new IllegalStateException("업데이트 실패");
        }
    }

    public void failConnection(Long connectionId){
        int result=myDataConnectionMapper.updateStatus(connectionId, ConnectionStatus.FAILED);

        if (result!=1){
            throw new IllegalStateException("마이데이터 연동 철회 실패");
        }
    }

    public Long getConnectedConnectionId(Long userId) {
        MyDataConnection connection = myDataConnectionMapper.findByUserId(userId);

        if (connection == null) {
            throw new IllegalStateException("마이데이터 연동 정보가 없습니다.");
        }
        if (connection.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            throw new IllegalStateException("마이데이터가 연결된 상태가 아닙니다.");
        }

        return connection.getConnectionId();
    }
}