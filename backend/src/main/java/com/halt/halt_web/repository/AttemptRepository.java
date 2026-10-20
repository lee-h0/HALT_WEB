package com.halt.halt_web.repository;

import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /** 회원의 진행 중 응시 (회원당 최대 1건) */
    Optional<Attempt> findByUserUserIdAndStatus(Long userId, AttemptStatus status);

    /** 회원의 가장 최근 응시 (대시보드 "이어서 응시" 판단용) */
    Optional<Attempt> findFirstByUserUserIdOrderByStartedAtDesc(Long userId);

    /** 응시와 함께 회원, 문제를 한 번에 읽는다 (트랜잭션 밖에서 문제 정보를 쓰기 위해) */
    @EntityGraph(attributePaths = {"user", "problem"})
    Optional<Attempt> findWithProblemByAttemptId(Long attemptId);
}
