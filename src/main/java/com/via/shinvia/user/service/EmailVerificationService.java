package com.via.shinvia.user.service;

import com.via.shinvia.user.domain.User;
import com.via.shinvia.user.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    public static final String VERIFIED_EMAIL_KEY="VERIFIED_EMAIL";
    public static final String PASSWORD_RESET_VERIFIED_EMAIL_KEY = "PASSWORD_RESET_VERIFIED_EMAIL";
    private static final Duration CODE_TTL=Duration.ofMinutes(5);

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode (String email, HttpSession session) {
        String normalizedEmail = normalizedEmail(email);
        validateDuplicateEmail(email);
        validateVerifiedEmail(normalizedEmail, email);

        String code = createVerificationCode();

        session.removeAttribute(VERIFIED_EMAIL_KEY);

        sendVerificationMail(normalizedEmail, code);

        String key="email-verification:code:"+normalizedEmail;

        redisTemplate.opsForValue().set(
                key,
                code,
                Duration.ofMinutes(5)
        );

    }
    private void validateVerifiedEmail(String loginEmail, String verifiedEmail) {
        if(verifiedEmail==null || !loginEmail.equalsIgnoreCase(normalizedEmail(verifiedEmail))) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요.");
        }
    }

    private void validateDuplicateEmail(String loginEmail) {
        if (userMapper.existsByLoginEmail(loginEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
    }

    public void verifyCode(String email, String inputCode, HttpSession session) {
        String normalizedEmail=normalizedEmail(email);
        String key = createCodeKey(normalizedEmail);
        String savedCode=redisTemplate.opsForValue().get(key);

        if(!StringUtils.hasText(savedCode)) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 발급되지 않았습니다.");
        }
        if(!savedCode.equals(inputCode.trim())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        redisTemplate.delete(key);
        session.setAttribute(VERIFIED_EMAIL_KEY, normalizedEmail);
    }


    private void sendVerificationMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[SinVia] 이메일 인증번호");
        message.setText(
                "회원가입 인증번호는 "+code+"입니다. \n" + "인증번호는 5분동안 유효합니다."
        );
        mailSender.send(message);
    }

    private String createVerificationCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private String normalizedEmail(String email) {
        if(!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String createCodeKey(String email) {
        return "email-verification:code:"+email;
    }

    public void sendPasswordResetCode(String email, HttpSession session) {
        String normalizedEmail = normalizedEmail(email);
        User user = userMapper.findByLoginEmail(normalizedEmail);

        if (user == null) {
            throw new IllegalArgumentException("가입된 이메일을 찾을 수 없습니다.");
        }

        String code = createVerificationCode();
        session.removeAttribute(PASSWORD_RESET_VERIFIED_EMAIL_KEY);
        sendVerificationMail(normalizedEmail, code);

        String key = "password-reset:code:" + normalizedEmail;

        redisTemplate.opsForValue().set(key, code, CODE_TTL);
    }

    public void verifyPasswordResetCode(String email, String inputCode, HttpSession session) {
        String normalizedEmail = normalizedEmail(email);
        String key = "password-reset:code:" + normalizedEmail;

        String savedCode = redisTemplate.opsForValue().get(key);

        if (!StringUtils.hasText(savedCode)) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 발급되지 않았습니다.");
        }

        if (!savedCode.equals(inputCode.trim())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        redisTemplate.delete(key);

        session.setAttribute(PASSWORD_RESET_VERIFIED_EMAIL_KEY, normalizedEmail);
    }

    private String createPasswordResetCodeKey(String email) {
        return "password-reset:code:" + email;
    }
}
