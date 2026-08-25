package com.via.shinvia.mydata.client;


import com.via.shinvia.mydata.client.dto.response.LoanListResponseDto;
import com.via.shinvia.mydata.config.MyDataProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MyDataLoanClient {

    private final RestClient restClient;
    private final MyDataProperties myDataProperties;



    public MyDataLoanClient(RestClient.Builder builder, @Value("${shinvia-client.mock.url}") String mockUrl,
                            MyDataProperties myDataProperties)  {

        this.restClient =builder.baseUrl(mockUrl).build();
        this.myDataProperties = myDataProperties;
    }

    public LoanListResponseDto getLoans(String accessToken) {


        return restClient.get().uri(uriBuilder-> uriBuilder.path("/v2/loan/accounts").queryParam("org_code", myDataProperties.getOrgCode())
                .queryParam("search_timestamp", "0").queryParam("limit", 100).build()

        ). header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken).retrieve().body(LoanListResponseDto.class);
    }

}
