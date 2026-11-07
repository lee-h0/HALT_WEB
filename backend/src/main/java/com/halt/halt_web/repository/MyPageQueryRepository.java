package com.halt.halt_web.repository;

import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.DailyRecommendation;
import com.halt.halt_web.domain.Problem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 마이페이지(대시보드, 오늘의 추천) 전용 조회.
 * 화면에 필요한 연관 엔티티를 join fetch로 한 번에 읽어서, 트랜잭션 밖에서 써도 지연 로딩 오류가 나지 않게 한다.
 */
public interface MyPageQueryRepository extends Repository<Attempt, Long> {

    /** 상태별 응시 수 (제출 횟수, 정답 횟수 계산용) */
    long countByUserUserIdAndStatus(Long userId, AttemptStatus status);

    /** 최근 제출 기록 (문제, 난이도까지 함께) */
    @Query("""
            select a from Attempt a
            join fetch a.problem p
            join fetch p.difficultySetting
            where a.user.userId = :userId and a.status in :statuses
            order by a.submittedAt desc, a.attemptId desc
            """)
    List<Attempt> findRecentSubmissions(@Param("userId") Long userId,
                                        @Param("statuses") Collection<AttemptStatus> statuses,
                                        Pageable pageable);

    /** 이어서 응시: 아직 못 푼 (공개 중인) 문제의 가장 최근 응시 */
    @Query("""
            select a from Attempt a
            join fetch a.problem p
            join fetch p.difficultySetting
            where a.user.userId = :userId
              and p.active = true
              and not exists (
                  select b from UserProblemBest b
                  where b.id.userId = :userId and b.id.problemId = p.problemId)
            order by a.startedAt desc, a.attemptId desc
            """)
    List<Attempt> findLatestUnsolvedAttempts(@Param("userId") Long userId, Pageable pageable);

    /** 공개 중인 문제 전체 (난이도 설정까지 함께) */
    @Query("select p from Problem p join fetch p.difficultySetting where p.active = true order by p.problemId")
    List<Problem> findActiveProblemsWithDifficulty();

    /** 문제별 제출 결과 수: [problemId, status, count] */
    @Query("""
            select a.problem.problemId, a.status, count(a) from Attempt a
            where a.user.userId = :userId and a.status in :statuses
            group by a.problem.problemId, a.status
            """)
    List<Object[]> countSubmissionsByProblem(@Param("userId") Long userId,
                                             @Param("statuses") Collection<AttemptStatus> statuses);

    /** 오늘 이미 만든 추천 (문제, 난이도까지 함께) */
    @Query("""
            select r from DailyRecommendation r
            join fetch r.problem p
            join fetch p.difficultySetting
            where r.id.userId = :userId and r.id.recDate = :date
            """)
    Optional<DailyRecommendation> findRecommendation(@Param("userId") Long userId, @Param("date") LocalDate date);
}
