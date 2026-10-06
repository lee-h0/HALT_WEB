package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 난이도별 고정값 (EASY / MEDIUM / HARD 3행) */
@Entity
@Table(name = "difficulty_settings")
@Getter @Setter @NoArgsConstructor
public class DifficultySetting {

    @Id
    @Column(name = "difficulty", length = 10)
    private String difficulty;

    /** AI 사용량 100%에 해당하는 토큰 수 */
    @Column(name = "token_limit", nullable = false)
    private int tokenLimit;

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    /** 한도 초과 시 지급하는 최하점 */
    @Column(name = "min_points", nullable = false)
    private int minPoints;

    /** 풀이 열람 시 차감 포인트 */
    @Column(name = "unlock_cost", nullable = false)
    private int unlockCost;
}
