package com.via.shinvia.oauth2.domain;

public enum OAuth2LoginStatus {
    EXISTING_USER, // 이미 카카오 연결까지 되어 있음 -> 바로 로그인
    LINK_REQUIRED, // 일반 회원은 있는데 카카오 연결은 없음 -> 기존 계정과 연결
    NEW_USER  //둘 다 없음 -> 소셜 회원가입
}
