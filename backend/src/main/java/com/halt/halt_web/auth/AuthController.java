package com.halt.halt_web.auth;

import com.halt.halt_web.auth.dto.LoginRequest;
import com.halt.halt_web.auth.dto.LoginResponse;
import com.halt.halt_web.auth.dto.MeResponse;
import com.halt.halt_web.auth.dto.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 회원가입. 성공하면 201 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
    }

    /** 로그인. 성공하면 Access Token을 돌려준다 */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 내 정보. 토큰이 있어야 호출된다 (JwtAuthenticationFilter가 넣어 준 회원 번호를 받는다) */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return authService.me(userId);
    }
}
