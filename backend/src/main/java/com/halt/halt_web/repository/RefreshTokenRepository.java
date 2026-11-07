package com.halt.halt_web.repository;

import com.halt.halt_web.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** 같은 토큰으로 동시에 재발급 요청이 와도 한 번에 하나씩 처리되도록 행을 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 묶음 전체를 무효로 한다 (탈취 의심, 로그아웃) */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);

    /** 만료된 지 오래된 토큰 정리 */
    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId and t.expiresAt < :before")
    int deleteExpired(@Param("userId") Long userId, @Param("before") LocalDateTime before);
}
