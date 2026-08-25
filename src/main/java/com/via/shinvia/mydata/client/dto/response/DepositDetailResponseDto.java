package com.via.shinvia.mydata.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DepositDetailResponseDto { //잔액 상세 응답
    private String rspCode;
    private String rspMsg;
    private Integer detailCnt;
    private List<DepositDetailItemDto> detailList;
}
