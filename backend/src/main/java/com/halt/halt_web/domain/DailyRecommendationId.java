package com.halt.halt_web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class DailyRecommendationId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rec_date")
    private LocalDate recDate;
}
