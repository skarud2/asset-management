package com.via.shinvia.client.card.list.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CardInfoDto {

    @JsonProperty("card_id")
    private String cardId;

    // 카드를 발급한 금융기관 코드(목서버는 bank_code_std를 10자리로 LPAD해서 내려줌).
    // financial_institution.org_code와 비교할 땐 자릿수가 다를 수 있어 숫자로 정규화해서 조회한다
    // (CardMapper.findInstitutionIdByOrgCode 참고).
    @JsonProperty("institution_id")
    private String institutionId;

    @JsonProperty("card_num")
    private String cardNum;

    @JsonProperty("is_consent")
    private Boolean isConsent;

    @JsonProperty("card_name")
    private String cardName;

    @JsonProperty("card_member")
    private String cardMember;
}
