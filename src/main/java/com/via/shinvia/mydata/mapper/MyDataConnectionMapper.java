package com.via.shinvia.mydata.mapper;

import com.via.shinvia.mydata.domain.ConnectionStatus;
import com.via.shinvia.mydata.domain.MyDataConnection;
import com.via.shinvia.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyDataConnectionMapper {
    int insertMyDataConnection(MyDataConnection myDataConnection);
    MyDataConnection findByUserId(@Param("userId") Long userId);
    int updateConnected(@Param("connectionId") Long connectionId);
    int updateStatus(@Param("connectionId") Long connectionId,
                     @Param("connectionStatus") ConnectionStatus connectionStatus);
    Long findConnectionIdByUserId(@Param("userId") Long userId);
}
