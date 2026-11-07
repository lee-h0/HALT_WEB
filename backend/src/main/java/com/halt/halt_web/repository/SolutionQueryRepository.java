package com.halt.halt_web.repository;

import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.UserProblemBest;
import com.halt.halt_web.domain.UserProblemId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 다른 사람 풀이 화면 전용 조회.
 * 공개하는 풀이는 user_problem_best에 기록된 "처음 맞힌 제출"뿐이다.
 */
public interface SolutionQueryRepository extends Repository<UserProblemBest, UserProblemId> {

    /** 공개 중인 문제 (난이도 설정까지 함께) */
    @Query("select p from Problem p join fetch p.difficultySetting where p.problemId = :problemId and p.active = true")
    Optional<Problem> findActiveProblem(@Param("problemId") Long problemId);

    /** 문제의 풀이 목록: 점수 높은 순 → 빨리 푼 순 → 먼저 푼 순 */
    @Query("""
            select b from UserProblemBest b
            join fetch b.bestAttempt a
            join fetch a.user
            where b.id.problemId = :problemId
            order by b.bestScore desc, a.elapsedSec asc, b.firstSolvedAt asc
            """)
    List<UserProblemBest> findSolutions(@Param("problemId") Long problemId);

    /** 풀이 한 건 (공개 대상인 응시만 찾는다) */
    @Query("""
            select b from UserProblemBest b
            join fetch b.bestAttempt a
            join fetch a.user
            join fetch a.problem p
            join fetch p.difficultySetting
            where a.attemptId = :attemptId
            """)
    Optional<UserProblemBest> findSolutionByAttemptId(@Param("attemptId") Long attemptId);
}
