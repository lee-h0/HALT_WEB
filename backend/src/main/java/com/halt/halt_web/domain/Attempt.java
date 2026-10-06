package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 응시 기록 (문제 풀이 화면에 들어갈 때마다 1행) */
@Entity
@Table(name = "attempts")
@Getter @Setter @NoArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "elapsed_sec", nullable = false)
    private int elapsedSec = 0;

    /** 누적 토큰. AI 사용량 % = tokensUsed / 난이도의 tokenLimit */
    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed = 0;

    /** 지시 횟수 (화면 표시용, 점수 미반영) */
    @Column(name = "prompt_count", nullable = false)
    private int promptCount = 0;

    /** 파일 변경 횟수 (화면 표시용, 점수 미반영) */
    @Column(name = "file_change_count", nullable = false)
    private int fileChangeCount = 0;

    /** 이 응시로 산출된 점수. 채점 전에는 null */
    @Column(name = "score")
    private Integer score;
}
