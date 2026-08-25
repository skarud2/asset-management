package com.via.shinvia.mydata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSummaryDto {
    private String cardName;
    private String maskedCardNumber;
}
