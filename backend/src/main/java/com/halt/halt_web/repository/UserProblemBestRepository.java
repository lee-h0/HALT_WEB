package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.UserProblemBest;
import com.halt.halt_web.domain.UserProblemId;

import java.util.List;

public interface UserProblemBestRepository extends JpaRepository<UserProblemBest, UserProblemId> {

    /** 다른 사람 풀이 목록: 문제별 최고 기록을 점수 순으로 */
    List<UserProblemBest> findByIdProblemIdOrderByBestScoreDesc(Long problemId);

    /** 회원이 푼 문제 목록 */
    List<UserProblemBest> findByIdUserId(Long userId);
}
