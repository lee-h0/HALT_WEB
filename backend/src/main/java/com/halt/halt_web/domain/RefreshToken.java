package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 로그인 유지용 Refresh Token.
 * 토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다 (DB가 유출돼도 토큰을 쓸 수 없게).
 * 한 번 쓰면 새 토큰으로 바꾸고(회전), 같은 로그인에서 이어진 토큰은 family_id로 묶는다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 토큰 원문의 SHA-256 (16진수 64자) */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** 같은 로그인에서 회전으로 이어진 토큰 묶음 (UUID) */
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 회전, 로그아웃, 탈취 감지로 무효가 된 시각. 쓸 수 있으면 null */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
