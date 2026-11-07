package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** 제출한 응시의 AI 피드백 (검증 역량 평가). 응시당 1건 */
@Entity
@Table(name = "attempt_feedbacks")
@Getter @Setter @NoArgsConstructor
public class AttemptFeedback {

    @Id
    @Column(name = "attempt_id")
    private Long attemptId;

    /** 조건 명시 0~5. AI를 쓰지 않았으면 null */
    @Column(name = "spec_score")
    private Integer specScore;

    /** 지시 효율 0~5. AI를 쓰지 않았으면 null */
    @Column(name = "efficiency_score")
    private Integer efficiencyScore;

    /** 결과 정확성 0~5 */
    @Column(name = "accuracy_score", nullable = false)
    private int accuracyScore;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    /** 화면에 보여줄 피드백 전체 */
    @Nationalized
    @Column(name = "feedback_json", nullable = false, columnDefinition = "nvarchar(max)")
    private String feedbackJson;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
