package com.via.shinvia.stresstest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 개인 재무 스트레스테스트 API를 브라우저에서 수동 테스트하기 위한 화면 (로컬 테스트 전용)
@Controller
public class PersonalStressTestViewController {

    @GetMapping("/stress-test/personal")
    public String personalStressTestPage() {
        return "stresstest/personal";
    }
}
