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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 아이디가 없는 경우와 비밀번호가 틀린 경우를 같은 메시지로 응답한다 (아이디 존재 여부를 알려주지 않기 위해)
        User user = userRepository.findByLoginId(request.loginId())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        String accessToken = jwtTokenProvider.createAccessToken(user);
        return new LoginResponse(accessToken, jwtTokenProvider.getAccessTokenValiditySec(),
                user.getUserId(), user.getNickname());
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
        return MeResponse.from(user);
    }
}
