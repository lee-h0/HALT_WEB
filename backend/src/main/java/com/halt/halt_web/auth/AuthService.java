package com.halt.halt_web.auth;

import com.halt.halt_web.auth.dto.LoginRequest;
import com.halt.halt_web.auth.dto.LoginResponse;
import com.halt.halt_web.auth.dto.MeResponse;
import com.halt.halt_web.auth.dto.SignupRequest;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.User;
import com.halt.halt_web.repository.UserRepository;
import com.halt.halt_web.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** 로그인·재발급 결과: 응답 본문(Access Token)과 쿠키로 보낼 Refresh Token */
    public record TokenResult(LoginResponse body, String refreshToken) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setLoginId(request.loginId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        userRepository.save(user);
    }

    public TokenResult login(LoginRequest request) {
        // 아이디가 없는 경우와 비밀번호가 틀린 경우를 같은 메시지로 응답한다 (아이디 존재 여부를 알려주지 않기 위해)
        User user = userRepository.findByLoginId(request.loginId())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        RefreshTokenService.Issued refresh = refreshTokenService.issueNew(user.getUserId());
        return new TokenResult(toResponse(user), refresh.rawToken());
    }

    /** Refresh Token으로 Access Token을 다시 받는다 (Refresh Token도 새것으로 바뀐다) */
    public TokenResult refresh(String rawRefreshToken) {
        RefreshTokenService.Issued refresh = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(refresh.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
        return new TokenResult(toResponse(user), refresh.rawToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
        return MeResponse.from(user);
    }

    private LoginResponse toResponse(User user) {
        return new LoginResponse(jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.getAccessTokenValiditySec(), user.getUserId(), user.getNickname());
    }
}
