package com.halt.halt_web.auth;

import com.halt.halt_web.auth.dto.LoginRequest;
import com.halt.halt_web.auth.dto.LoginResponse;
import com.halt.halt_web.auth.dto.MeResponse;
import com.halt.halt_web.auth.dto.SignupRequest;
import com.halt.halt_web.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService,
                          RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    /** 회원가입. 성공하면 201 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
    }

    /** 로그인. Access Token은 본문으로, Refresh Token은 HttpOnly 쿠키로 준다 */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenResult result = authService.login(request);
        return withRefreshCookie(result);
    }

    /**
     * Access Token 재발급. 쿠키의 Refresh Token을 쓰고 새것으로 바꾼다.
     * - 쿠키가 없으면 204 (로그인하지 않은 방문자. 브라우저 콘솔에 오류가 찍히지 않게)
     * - 만료·무효면 401과 함께 쿠키를 지운다
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        try {
            return withRefreshCookie(authService.refresh(rawRefreshToken));
        } catch (ApiException e) {
            return ResponseEntity.status(e.getStatus())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /** 로그아웃. Refresh Token을 무효로 하고 쿠키를 지운다 (Access Token은 프론트가 메모리에서 지운다) */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            authService.logout(rawRefreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                .build();
    }

    /** 내 정보. 토큰이 있어야 호출된다 (JwtAuthenticationFilter가 넣어 준 회원 번호를 받는다) */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return authService.me(userId);
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(AuthService.TokenResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.create(result.refreshToken(), refreshTokenService.validity()))
                .body(result.body());
    }
}
