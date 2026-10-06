package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;

import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /** 회원의 진행 중 응시 (회원당 최대 1건) */
    Optional<Attempt> findByUserUserIdAndStatus(Long userId, AttemptStatus status);

    /** 회원의 가장 최근 응시 (대시보드 "이어서 응시" 판단용) */
    Optional<Attempt> findFirstByUserUserIdOrderByStartedAtDesc(Long userId);
}
