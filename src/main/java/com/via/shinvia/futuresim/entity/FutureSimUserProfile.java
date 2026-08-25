package com.via.shinvia.futuresim.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// "지금 내 상태" 비교 패널에 필요한 app_user 최소 조회 컬럼만 담는 모델.
// user/ 도메인의 User.java에는 household_size가 없고(이번 세션에서 새로 추가한 컬럼이라 아직 반영 안 됨),
// user/ 기존 파일을 건드리지 않기로 해서 futuresim 전용으로 별도 조회한다.
@Getter
@Setter
@NoArgsConstructor
public class FutureSimUserProfile {

    private LocalDate birthDate;

    private String householdSize;
}
