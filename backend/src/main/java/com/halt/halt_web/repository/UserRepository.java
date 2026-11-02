package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.User;
import com.halt.halt_web.domain.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    // ---------- 랭킹 (관리자 제외, 0인 회원 제외) ----------

    /** 포인트 랭킹 1~10등. 같은 포인트면 많이 푼 사람, 그다음 먼저 가입한 사람 순으로 보여 준다 */
    List<User> findTop10ByRoleAndTotalPointsGreaterThanOrderByTotalPointsDescSolvedCountDescUserIdAsc(
            UserRole role, int totalPoints);

    /** 푼 문제 수 랭킹 1~10등. 같은 수면 포인트가 많은 사람, 그다음 먼저 가입한 사람 순 */
    List<User> findTop10ByRoleAndSolvedCountGreaterThanOrderBySolvedCountDescTotalPointsDescUserIdAsc(
            UserRole role, int solvedCount);

    /** 내 포인트 순위 = 나보다 포인트가 많은 회원 수 + 1 (랭킹 대상 인원 수를 셀 때도 쓴다) */
    long countByRoleAndTotalPointsGreaterThan(UserRole role, int totalPoints);

    /** 내 풀이 수 순위 = 나보다 많이 푼 회원 수 + 1 */
    long countByRoleAndSolvedCountGreaterThan(UserRole role, int solvedCount);
}
