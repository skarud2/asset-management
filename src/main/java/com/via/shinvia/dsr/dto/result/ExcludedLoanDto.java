package com.via.shinvia.dsr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExcludedLoanDto {
    private Long loanAccountId;
    private String loanType;
    private String reason;
}
