package com.via.shinvia.welfare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BokjiroApiResponse {

    private int totalCount;
    private int pageNo;
    private int numOfRows;
    private String resultCode;
    private String resultMessage;
    private List<BokjiroServiceItem> servList;
}
