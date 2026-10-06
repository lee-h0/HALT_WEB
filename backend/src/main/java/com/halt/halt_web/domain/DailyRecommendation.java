package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Nationalized;

/** AI 추천 오늘의 문제 (회원마다 하루 1행) */
@Entity
@Table(name = "daily_recommendations")
@Getter @Setter @NoArgsConstructor
public class DailyRecommendation {

    @EmbeddedId
    private DailyRecommendationId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Nationalized
    @Column(name = "reason", length = 300)
    private String reason;
}
