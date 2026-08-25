package com.via.shinvia.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 요청 경로에 맞는 상단 서비스 대주제 제공
@ControllerAdvice
public class LayoutModelAdvice {

    @ModelAttribute("serviceName")
    public String serviceName(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/financial-policy")) {
            return "금융정책 안내";
        }

        if (uri.startsWith("/policy-support")
                || uri.startsWith("/asset-products")
                || uri.startsWith("/social-finance")
                || uri.startsWith("/welfare-support")
                || uri.startsWith("/policy/recommendation")) {
            return "정책금융상품";
        }

        if (uri.startsWith("/loans") || uri.startsWith("/loan-analysis")) {
            return "대출";
        }

        if (uri.startsWith("/lifecycle") || uri.startsWith("/surplus-funds")) {
            return "금융 라이프 플랜";
        }

        return "금융 진단센터";
    }
}
