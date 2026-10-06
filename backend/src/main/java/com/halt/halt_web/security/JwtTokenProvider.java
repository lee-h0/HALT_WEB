package com.halt.halt_web.security;

import com.halt.halt_web.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Access Token 발급과 검증 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenValidity;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-minutes:30}") long accessTokenMinutes) {
        // 서명 키는 32바이트 이상이어야 한다. 짧으면 서버 시작 시 오류가 난다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofMinutes(accessTokenMinutes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(key)
                .compact();
    }

    /** 서명과 만료를 검증하고 내용을 꺼낸다. 문제가 있으면 JwtException을 던진다 */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenValiditySec() {
        return accessTokenValidity.toSeconds();
    }
}
