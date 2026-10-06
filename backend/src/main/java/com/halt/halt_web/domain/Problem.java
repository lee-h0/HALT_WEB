package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** 문제 */
@Entity
@Table(name = "problems")
@Getter @Setter @NoArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "problem_id")
    private Long problemId;

    /** Git 문제 폴더 이름. 중복 등록 방지용 키 */
    @Column(name = "slug", nullable = false, length = 50, unique = true)
    private String slug;

    @Nationalized
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 난이도. 토큰 한도와 배점은 여기서 가져온다 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "difficulty", nullable = false)
    private DifficultySetting difficultySetting;

    @Nationalized
    @Column(name = "description", nullable = false, columnDefinition = "nvarchar(max)")
    private String description;

    /** 지킬 조건 */
    @Nationalized
    @Column(name = "constraints_text", columnDefinition = "nvarchar(max)")
    private String constraintsText;

    /** 안 해도 되는 것 */
    @Nationalized
    @Column(name = "out_of_scope_text", columnDefinition = "nvarchar(max)")
    private String outOfScopeText;

    @Nationalized
    @Column(name = "test_command", nullable = false, length = 200)
    private String testCommand;

    /** false면 문제 목록에서 숨긴다 */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
