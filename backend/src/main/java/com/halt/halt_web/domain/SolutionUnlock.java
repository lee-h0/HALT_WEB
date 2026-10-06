package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 못 푼 상태에서 포인트를 내고 풀이를 연 기록. 이 행이 있는 문제는 맞혀도 점수가 없다 */
@Entity
@Table(name = "solution_unlocks")
@Getter @Setter @NoArgsConstructor
public class SolutionUnlock {

    @EmbeddedId
    private UserProblemId id;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    @CreationTimestamp
    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private LocalDateTime unlockedAt;

    public SolutionUnlock(Long userId, Long problemId, int pointsSpent) {
        this.id = new UserProblemId(userId, problemId);
        this.pointsSpent = pointsSpent;
    }
}
