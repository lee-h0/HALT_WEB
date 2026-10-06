package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Nationalized;

/** 제출 시점의 풀이 파일 (다른 사람 풀이 화면에 표시) */
@Entity
@Table(name = "attempt_files")
@Getter @Setter @NoArgsConstructor
public class AttemptFile {

    @EmbeddedId
    private AttemptFileId id;

    @Nationalized
    @Column(name = "content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    public AttemptFile(Long attemptId, String filePath, String content) {
        this.id = new AttemptFileId(attemptId, filePath);
        this.content = content;
    }
}
