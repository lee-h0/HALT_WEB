package com.halt.halt_web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.io.Serializable;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class AttemptFileId implements Serializable {

    @Column(name = "attempt_id")
    private Long attemptId;

    @Nationalized
    @Column(name = "file_path", length = 260)
    private String filePath;
}
