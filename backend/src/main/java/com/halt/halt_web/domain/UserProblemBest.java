package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 회원·문제별 최고 기록 1건. 포인트와 푼 문제 수는 이 테이블 기준으로만 올린다 */
@Entity
@Table(name = "user_problem_best")
@Getter @Setter @NoArgsConstructor
public class UserProblemBest {

    @EmbeddedId
    private UserProblemId id;

    /** 최고 점수를 낸 응시 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "best_attempt_id", nullable = false)
    private Attempt bestAttempt;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @CreationTimestamp
    @Column(name = "first_solved_at", nullable = false, updatable = false)
    private LocalDateTime firstSolvedAt;

    public UserProblemBest(Long userId, Long problemId, Attempt bestAttempt, int bestScore) {
        this.id = new UserProblemId(userId, problemId);
        this.bestAttempt = bestAttempt;
        this.bestScore = bestScore;
    }
}
