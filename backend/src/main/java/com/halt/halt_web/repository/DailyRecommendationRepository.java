package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.DailyRecommendation;
import com.halt.halt_web.domain.DailyRecommendationId;

public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, DailyRecommendationId> {
}
