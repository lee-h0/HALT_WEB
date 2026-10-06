package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** AI 지시 기록 (지시 1번에 1행) */
@Entity
@Table(name = "prompt_logs")
@Getter @Setter @NoArgsConstructor
public class PromptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id")
    private Long promptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @Nationalized
    @Column(name = "prompt_text", nullable = false, columnDefinition = "nvarchar(max)")
    private String promptText;

    @Nationalized
    @Column(name = "response_text", columnDefinition = "nvarchar(max)")
    private String responseText;

    /** 사용한 LLM 모델명 */
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
