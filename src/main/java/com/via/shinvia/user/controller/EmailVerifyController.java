package com.via.shinvia.user.controller;

import com.via.shinvia.user.dto.EmailSendRequestDto;
import com.via.shinvia.user.dto.EmailVerifyRequestDto;
import com.via.shinvia.user.service.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email-verify")
@RequiredArgsConstructor
public class EmailVerifyController {
    private final EmailVerificationService verificationService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendCode(
            @Valid @RequestBody EmailSendRequestDto request,
            HttpSession session) {
        verificationService.sendCode(request.email(), session);
        return ResponseEntity.ok(Map.of("message","인증번호를 전송했습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyCode(
            @Valid @RequestBody EmailVerifyRequestDto request,
            HttpSession session){
        verificationService.verifyCode(request.email(), request.code(), session);

        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/password/send")
    public ResponseEntity<Map<String, String>> sendPasswordResetCode(
            @Valid @RequestBody EmailSendRequestDto request,
            HttpSession session
    ) {
        verificationService.sendPasswordResetCode(request.email(), session);

        return ResponseEntity.ok(Map.of("message", "인증번호를 전송했습니다."));
    }

    @PostMapping("/password/verify")
    public ResponseEntity<Map<String, String>> verifyPasswordResetCode(
            @Valid @RequestBody EmailVerifyRequestDto request,
            HttpSession session
    ) {
        verificationService.verifyPasswordResetCode(request.email(), request.code(), session);

        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

}
