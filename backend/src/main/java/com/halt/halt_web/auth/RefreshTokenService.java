package com.halt.halt_web.auth;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.RefreshToken;
import com.halt.halt_web.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh Token 발급, 회전, 폐기.
 *
 * - 유효 기간: app.auth.refresh-token-minutes (기본 60분). 쓸 때마다 새 토큰을 주므로
 *   "마지막으로 사용한 뒤 60분 동안 아무것도 안 하면 다시 로그인"이 된다.
 * - 회전: 재발급할 때마다 쓴 토큰은 무효로 하고 새 토큰을 준다.
 * - 탈취 감지: 이미 무효가 된 토큰이 다시 오면 누군가 훔쳐 쓴 것으로 보고 같은 묶음을 모두 무효로 한다.
 *   단, 탭 여러 개가 동시에 재발급하는 경우를 위해 무효가 된 지 30초 안이면 봐준다.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);
    private static final String EXPIRED_MESSAGE = "로그인이 만료되었습니다. 다시 로그인하세요.";

    /** 발급 결과. rawToken은 쿠키로만 내보내고 저장하지 않는다 */
    public record Issued(String rawToken, Long userId) {
    }

    private final RefreshTokenRepository repository;
    private final Duration validity;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.auth.refresh-token-minutes:60}") long refreshTokenMinutes) {
        this.repository = repository;
        this.validity = Duration.ofMinutes(refreshTokenMinutes);
    }

    public Duration validity() {
        return validity;
    }

    /** 로그인: 새 묶음으로 발급 */
    @Transactional
    public Issued issueNew(Long userId) {
        repository.deleteExpired(userId, LocalDateTime.now().minusDays(1));
        return issue(userId, UUID.randomUUID().toString());
    }

    /** 재발급: 쓴 토큰을 무효로 하고 같은 묶음으로 새 토큰을 준다 */
    @Transactional(noRollbackFor = ApiException.class)
    public Issued rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, EXPIRED_MESSAGE));

        if (token.getRevokedAt() != null) {
            if (token.getRevokedAt().isAfter(now.minus(REUSE_GRACE))) {
                // 방금 다른 탭이 바꾼 토큰: 묶음은 살려 두고 새 토큰을 하나 더 준다
                return issue(token.getUserId(), token.getFamilyId());
            }
            int revoked = repository.revokeFamily(token.getFamilyId(), now);
            log.warn("무효가 된 Refresh Token 재사용 감지: userId={}, 묶음 {}개 무효 처리", token.getUserId(), revoked);
            throw new ApiException(HttpStatus.UNAUTHORIZED, EXPIRED_MESSAGE);
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, EXPIRED_MESSAGE);
        }

        token.setRevokedAt(now);
        return issue(token.getUserId(), token.getFamilyId());
    }

    /** 로그아웃: 이 토큰의 묶음을 모두 무효로 한다. 없는 토큰이면 아무것도 안 한다 */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), LocalDateTime.now()));
    }

    private Issued issue(Long userId, String familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(LocalDateTime.now().plus(validity));
        repository.save(token);
        return new Issued(raw, userId);
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
