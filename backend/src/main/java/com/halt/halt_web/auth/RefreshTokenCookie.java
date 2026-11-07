package com.halt.halt_web.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 쿠키.
 * - HttpOnly: 자바스크립트로 읽을 수 없다 (XSS로 훔치기 어렵다)
 * - SameSite=Strict: 다른 사이트에서 보낸 요청에는 붙지 않는다 (CSRF 방지)
 * - Path=/api/auth: 로그인 관련 요청에만 붙는다
 * - Secure: HTTPS에서만 전송. 개발(http://localhost) 중에는 false, 배포 시 app.auth.cookie-secure=true
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final boolean secure;

    public RefreshTokenCookie(@Value("${app.auth.cookie-secure:false}") boolean secure) {
        this.secure = secure;
    }

    public String create(String rawToken, Duration maxAge) {
        return base(rawToken).maxAge(maxAge).build().toString();
    }

    /** 쿠키 지우기 */
    public String clear() {
        return base("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
