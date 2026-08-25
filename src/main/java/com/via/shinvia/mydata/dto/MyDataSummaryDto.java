package com.via.shinvia.mydata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyDataSummaryDto {
    private List<AccountSummaryDto> accounts;
    private List<CardSummaryDto> cards;
}
