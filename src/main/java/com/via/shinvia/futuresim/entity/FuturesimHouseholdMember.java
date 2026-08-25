package com.via.shinvia.futuresim.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

// 가구원 1명(본인 제외)의 임시 입력값. DB에 저장하지 않고 세션에 List<FuturesimHouseholdMember>로만
// 담아둔다(HttpSession 직렬화 대비 Serializable 구현) — 최종 결과 저장 기능이 생기면 그때 같이 커밋될 값.
@Getter
@Setter
@NoArgsConstructor
public class FuturesimHouseholdMember implements Serializable {

    private String memberName;

    private String relationship;

    private Integer age;

    private BigDecimal annualIncome;
}
