package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    /** 포인트 랭킹 1~10등 */
    List<User> findTop10ByOrderByTotalPointsDesc();

    /** 푼 문제 수 랭킹 1~10등 */
    List<User> findTop10ByOrderBySolvedCountDesc();

    /** 내 포인트 순위 = 나보다 포인트가 많은 회원 수 + 1 */
    long countByTotalPointsGreaterThan(int totalPoints);

    /** 내 풀이 수 순위 = 나보다 많이 푼 회원 수 + 1 */
    long countBySolvedCountGreaterThan(int solvedCount);
}
