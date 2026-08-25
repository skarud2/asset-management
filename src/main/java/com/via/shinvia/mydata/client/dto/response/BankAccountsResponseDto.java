package com.via.shinvia.mydata.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BankAccountsResponseDto {  //계좌 목록 응답
    private Integer accountCnt;
    private List<AccountItemDto> accountList;
}
