package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Nationalized;

/** 문제 초기 파일과 숨김 테스트 */
@Entity
@Table(name = "problem_files")
@Getter @Setter @NoArgsConstructor
public class ProblemFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Nationalized
    @Column(name = "file_path", nullable = false, length = 260)
    private String filePath;

    @Nationalized
    @Column(name = "content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    /** true면 숨김 테스트. 응시자에게 보내지 않고 채점에만 쓴다 */
    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;
}
