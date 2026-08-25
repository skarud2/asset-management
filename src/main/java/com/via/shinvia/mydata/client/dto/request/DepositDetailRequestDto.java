package com.via.shinvia.mydata.client.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DepositDetailRequestDto {  //잔액 상세 요청
    private String orgCode;
    private String accountNum;
    private String seqno;
    private String searchTimestamp;
}
