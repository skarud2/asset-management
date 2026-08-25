package com.via.shinvia.mydata.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountItemDto {
    private String accountNum;
    private String seqno;
    private String prodName;
    private String accountType;
    private String accountStatus;
}
